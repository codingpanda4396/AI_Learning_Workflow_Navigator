package com.pandanav.learning.application.service.pathplan;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.pandanav.learning.application.service.llm.LlmJsonParser;
import com.pandanav.learning.application.service.llm.PromptOutputValidator;
import com.pandanav.learning.domain.enums.PlanMode;
import com.pandanav.learning.domain.enums.PlanSource;
import com.pandanav.learning.domain.enums.Stage;
import com.pandanav.learning.domain.llm.LlmGateway;
import com.pandanav.learning.domain.llm.PromptTemplateProvider;
import com.pandanav.learning.domain.llm.model.LlmPrompt;
import com.pandanav.learning.domain.llm.model.LlmTextResult;
import com.pandanav.learning.domain.llm.model.PersonalizedPathContext;
import com.pandanav.learning.domain.llm.model.PersonalizedPathPlan;
import com.pandanav.learning.domain.llm.model.PromptTemplateKey;
import com.pandanav.learning.domain.model.ConceptNode;
import com.pandanav.learning.domain.model.LearningEvent;
import com.pandanav.learning.domain.model.LearningSession;
import com.pandanav.learning.domain.model.Mastery;
import com.pandanav.learning.domain.model.TrainingAttemptSummary;
import com.pandanav.learning.domain.repository.ConceptNodeRepository;
import com.pandanav.learning.domain.repository.LearningEventRepository;
import com.pandanav.learning.domain.repository.MasteryRepository;
import com.pandanav.learning.domain.repository.TaskRepository;
import com.pandanav.learning.infrastructure.config.LlmProperties;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PersonalizedPathPlannerService {

    private static final int INSERTED_TASK_LIMIT = 3;
    private static final int RECENT_ATTEMPT_LIMIT = 6;
    private static final Set<String> BLOCKED_OBJECTIVE_WORDS = Set.of("色情", "仇恨", "暴力", "违法");

    private final ConceptNodeRepository conceptNodeRepository;
    private final MasteryRepository masteryRepository;
    private final TaskRepository taskRepository;
    private final LearningEventRepository learningEventRepository;
    private final LlmGateway llmGateway;
    private final PromptTemplateProvider promptTemplateProvider;
    private final PromptOutputValidator promptOutputValidator;
    private final LlmJsonParser llmJsonParser;
    private final LlmProperties llmProperties;
    private final ObjectMapper objectMapper;

    public PersonalizedPathPlannerService(
        ConceptNodeRepository conceptNodeRepository,
        MasteryRepository masteryRepository,
        TaskRepository taskRepository,
        LearningEventRepository learningEventRepository,
        LlmGateway llmGateway,
        PromptTemplateProvider promptTemplateProvider,
        PromptOutputValidator promptOutputValidator,
        LlmJsonParser llmJsonParser,
        LlmProperties llmProperties,
        ObjectMapper objectMapper
    ) {
        this.conceptNodeRepository = conceptNodeRepository;
        this.masteryRepository = masteryRepository;
        this.taskRepository = taskRepository;
        this.learningEventRepository = learningEventRepository;
        this.llmGateway = llmGateway;
        this.promptTemplateProvider = promptTemplateProvider;
        this.promptOutputValidator = promptOutputValidator;
        this.llmJsonParser = llmJsonParser;
        this.llmProperties = llmProperties;
        this.objectMapper = objectMapper;
    }

    public PersonalizedPlanResult plan(LearningSession session, PlanMode mode, boolean shadowMode) {
        List<ConceptNode> chapterNodes = conceptNodeRepository.findByChapterIdOrderByOrderNoAsc(session.getChapterId());
        List<ConceptNode> baseline = chapterNodes.stream()
            .sorted(Comparator.comparing(ConceptNode::getOrderNo).thenComparing(ConceptNode::getId))
            .toList();

        PersonalizedPathContext context = buildContext(session, chapterNodes);
        String fallbackReason = null;

        if (mode == PlanMode.RULE) {
            PersonalizedPlanResult result = buildRuleResult(baseline, List.of(), "Rule planner selected by mode.", List.of(), shadowMode);
            persistPlanEvent(session, mode, context, null, result, List.of("mode=rule"), List.of(), fallbackReason);
            return result;
        }

        boolean shouldTryLlm = mode == PlanMode.LLM || hasEnoughProfileData(context);
        if (!shouldTryLlm) {
            fallbackReason = "insufficient_profile_data";
            PersonalizedPlanResult result = buildRuleResult(
                baseline,
                List.of(),
                "Rule planner selected because profile data is insufficient for personalization.",
                List.of("LOW_PROFILE_DATA"),
                shadowMode
            );
            persistPlanEvent(session, mode, context, null, result, List.of(), List.of(), fallbackReason);
            return result;
        }

        if (!llmProperties.isEnabled() || !llmProperties.isReady()) {
            fallbackReason = "llm_not_ready";
            PersonalizedPlanResult result = buildRuleResult(
                baseline,
                List.of(),
                "Rule planner fallback because LLM is not ready.",
                List.of("LLM_NOT_READY"),
                shadowMode
            );
            persistPlanEvent(session, mode, context, null, result, List.of(), List.of(), fallbackReason);
            return result;
        }

        LlmPrompt prompt = promptTemplateProvider.buildPersonalizedPathPlanPrompt(context);
        LlmTextResult llmResult = null;
        List<String> schemaErrors = new ArrayList<>();
        List<String> businessErrors = new ArrayList<>();
        PersonalizedPathPlan parsedPlan = null;

        try {
            llmResult = llmGateway.generate(prompt);
            JsonNode parsed = llmJsonParser.parse(llmResult.text());
            schemaErrors = promptOutputValidator.validatePersonalizedPathPlan(parsed);
            if (!schemaErrors.isEmpty()) {
                throw new IllegalStateException("schema validation failed");
            }
            parsedPlan = toPathPlan(parsed);
            businessErrors = validateBusinessConstraints(parsedPlan, chapterNodes);
            if (!businessErrors.isEmpty()) {
                throw new IllegalStateException("business validation failed");
            }

            List<ConceptNode> orderedNodes = mergeOrderedNodes(parsedPlan.orderedNodes(), chapterNodes);
            List<PersonalizedPathPlan.InsertedTask> insertedTasks = parsedPlan.insertedTasks().stream()
                .limit(INSERTED_TASK_LIMIT)
                .toList();

            PersonalizedPlanResult result = shadowMode
                ? buildRuleResult(
                    baseline,
                    List.of(),
                    "Shadow mode: LLM plan generated but rule plan is applied.",
                    mergeRiskFlags(parsedPlan.riskFlags(), List.of("SHADOW_MODE")),
                    true
                )
                : new PersonalizedPlanResult(
                    PlanSource.LLM,
                    orderedNodes,
                    insertedTasks,
                    defaultText(parsedPlan.planReasoningSummary(), "LLM personalized plan applied."),
                    sanitizeRiskFlags(parsedPlan.riskFlags()),
                    prompt.promptKey(),
                    prompt.promptVersion(),
                    llmResult.provider(),
                    llmResult.model(),
                    false,
                    List.of(),
                    false
                );

            persistPlanEvent(session, mode, context, parsedPlan, result, schemaErrors, businessErrors, fallbackReason);
            return result;
        } catch (Exception ex) {
            fallbackReason = ex.getMessage() == null ? "llm_failed" : ex.getMessage();
            PersonalizedPlanResult result = buildRuleResult(
                baseline,
                List.of(),
                "Rule planner fallback after LLM planning failed validation.",
                List.of("LLM_PLAN_FALLBACK"),
                shadowMode
            );
            PersonalizedPlanResult withMeta = new PersonalizedPlanResult(
                result.source(),
                result.orderedNodes(),
                result.insertedTasks(),
                result.planReasoningSummary(),
                result.riskFlags(),
                prompt.promptKey(),
                prompt.promptVersion(),
                llmResult == null ? null : llmResult.provider(),
                llmResult == null ? null : llmResult.model(),
                true,
                mergeRiskFlags(schemaErrors, businessErrors),
                shadowMode
            );
            persistPlanEvent(session, mode, context, parsedPlan, withMeta, schemaErrors, businessErrors, fallbackReason);
            return withMeta;
        }
    }

    private PersonalizedPathContext buildContext(LearningSession session, List<ConceptNode> chapterNodes) {
        List<Mastery> masteryRows = masteryRepository.findByUserIdAndChapterId(session.getUserId(), session.getChapterId());
        Map<Long, BigDecimal> masteryByNode = masteryRows.stream()
            .collect(Collectors.toMap(Mastery::getNodeId, Mastery::getMasteryValue, (a, b) -> b, LinkedHashMap::new));

        List<TrainingAttemptSummary> attempts = taskRepository.findRecentTrainingAttempts(session.getId(), RECENT_ATTEMPT_LIMIT);
        List<String> recentErrorTags = attempts.stream()
            .flatMap(attempt -> attempt.errorTags().stream())
            .filter(tag -> tag != null && !tag.isBlank())
            .distinct()
            .limit(12)
            .toList();
        List<Integer> recentScores = attempts.stream()
            .map(TrainingAttemptSummary::score)
            .filter(score -> score != null)
            .limit(RECENT_ATTEMPT_LIMIT)
            .toList();

        List<PersonalizedPathContext.ChapterNodeSnapshot> nodeSnapshots = chapterNodes.stream()
            .sorted(Comparator.comparing(ConceptNode::getOrderNo).thenComparing(ConceptNode::getId))
            .map(node -> new PersonalizedPathContext.ChapterNodeSnapshot(node.getId(), node.getName(), node.getOrderNo()))
            .toList();

        return new PersonalizedPathContext(
            session.getGoalText(),
            estimateGoalDiagnosis(session.getGoalText()),
            masteryByNode,
            recentErrorTags,
            recentScores,
            nodeSnapshots
        );
    }

    private PersonalizedPathContext.GoalDiagnosisSnapshot estimateGoalDiagnosis(String goalText) {
        String safeGoal = goalText == null ? "" : goalText.trim();
        int specific = safeGoal.length() > 8 ? 16 : 10;
        int measurable = safeGoal.matches(".*(\\d+|%|score|points|次|题|分).*") ? 15 : 10;
        int achievable = safeGoal.length() <= 120 ? 16 : 12;
        int relevant = 15;
        int timeBound = safeGoal.matches(".*(day|week|month|天|周|月).*") ? 16 : 10;
        return new PersonalizedPathContext.GoalDiagnosisSnapshot(specific, measurable, achievable, relevant, timeBound);
    }

    private boolean hasEnoughProfileData(PersonalizedPathContext context) {
        long nonZeroMastery = context.masteryByNode().values().stream()
            .filter(value -> value != null && value.compareTo(BigDecimal.ZERO) > 0)
            .count();
        return nonZeroMastery >= 2 || context.recentScores().size() >= 2 || !context.recentErrorTags().isEmpty();
    }

    private PersonalizedPathPlan toPathPlan(JsonNode node) {
        List<PersonalizedPathPlan.OrderedNode> orderedNodes = new ArrayList<>();
        for (JsonNode item : node.path("ordered_nodes")) {
            orderedNodes.add(new PersonalizedPathPlan.OrderedNode(
                item.path("node_id").asLong(),
                item.path("priority").asInt(),
                item.path("reason").asText("")
            ));
        }

        List<PersonalizedPathPlan.InsertedTask> insertedTasks = new ArrayList<>();
        for (JsonNode item : node.path("inserted_tasks")) {
            insertedTasks.add(new PersonalizedPathPlan.InsertedTask(
                item.path("node_id").asLong(),
                item.path("stage").asText(""),
                item.path("objective").asText(""),
                item.path("trigger").asText("")
            ));
        }

        List<String> riskFlags = new ArrayList<>();
        for (JsonNode risk : node.path("risk_flags")) {
            if (risk.isTextual() && !risk.asText().isBlank()) {
                riskFlags.add(risk.asText().trim());
            }
        }

        return new PersonalizedPathPlan(
            orderedNodes,
            insertedTasks,
            node.path("plan_reasoning_summary").asText(""),
            riskFlags
        );
    }

    private List<String> validateBusinessConstraints(PersonalizedPathPlan plan, List<ConceptNode> chapterNodes) {
        List<String> errors = new ArrayList<>();
        Set<Long> allowedNodeIds = chapterNodes.stream().map(ConceptNode::getId).collect(Collectors.toSet());

        for (PersonalizedPathPlan.OrderedNode ordered : plan.orderedNodes()) {
            if (!allowedNodeIds.contains(ordered.nodeId())) {
                errors.add("ordered_nodes contains node outside chapter: " + ordered.nodeId());
            }
            if (ordered.priority() == null || ordered.priority() <= 0) {
                errors.add("ordered_nodes priority must be positive");
            }
        }

        if (plan.insertedTasks().size() > INSERTED_TASK_LIMIT) {
            errors.add("inserted_tasks exceeds limit " + INSERTED_TASK_LIMIT);
        }
        for (PersonalizedPathPlan.InsertedTask inserted : plan.insertedTasks()) {
            if (!allowedNodeIds.contains(inserted.nodeId())) {
                errors.add("inserted_tasks contains node outside chapter: " + inserted.nodeId());
            }
            String stageText = inserted.stage() == null ? "" : inserted.stage().trim().toUpperCase(Locale.ROOT);
            try {
                Stage stage = Stage.valueOf(stageText);
                if (stage != Stage.UNDERSTANDING && stage != Stage.TRAINING) {
                    errors.add("inserted_tasks stage must be UNDERSTANDING or TRAINING");
                }
            } catch (Exception ex) {
                errors.add("inserted_tasks stage is invalid: " + inserted.stage());
            }

            String objective = inserted.objective() == null ? "" : inserted.objective().trim();
            if (objective.length() < 10 || objective.length() > 200) {
                errors.add("inserted_tasks objective length must be 10-200");
            }
            if (containsBlockedWord(objective)) {
                errors.add("inserted_tasks objective contains blocked words");
            }
        }
        return errors;
    }

    private boolean containsBlockedWord(String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        return BLOCKED_OBJECTIVE_WORDS.stream().anyMatch(text::contains);
    }

    private List<ConceptNode> mergeOrderedNodes(
        List<PersonalizedPathPlan.OrderedNode> orderedNodes,
        List<ConceptNode> chapterNodes
    ) {
        Map<Long, ConceptNode> byId = chapterNodes.stream()
            .collect(Collectors.toMap(ConceptNode::getId, node -> node));

        List<Long> orderedIds = orderedNodes.stream()
            .sorted(Comparator.comparing(PersonalizedPathPlan.OrderedNode::priority))
            .map(PersonalizedPathPlan.OrderedNode::nodeId)
            .filter(byId::containsKey)
            .toList();

        LinkedHashSet<Long> finalOrder = new LinkedHashSet<>(orderedIds);
        chapterNodes.stream()
            .sorted(Comparator.comparing(ConceptNode::getOrderNo).thenComparing(ConceptNode::getId))
            .map(ConceptNode::getId)
            .forEach(finalOrder::add);

        return finalOrder.stream().map(byId::get).toList();
    }

    private PersonalizedPlanResult buildRuleResult(
        List<ConceptNode> baselineNodes,
        List<PersonalizedPathPlan.InsertedTask> insertedTasks,
        String summary,
        List<String> riskFlags,
        boolean shadowMode
    ) {
        return new PersonalizedPlanResult(
            PlanSource.RULE,
            baselineNodes,
            insertedTasks,
            summary,
            sanitizeRiskFlags(riskFlags),
            PromptTemplateKey.PATH_PLAN_V1.promptKey(),
            PromptTemplateKey.PATH_PLAN_V1.promptVersion(),
            null,
            null,
            false,
            List.of(),
            shadowMode
        );
    }

    private List<String> sanitizeRiskFlags(List<String> riskFlags) {
        if (riskFlags == null) {
            return List.of();
        }
        return riskFlags.stream()
            .filter(flag -> flag != null && !flag.isBlank())
            .map(String::trim)
            .distinct()
            .limit(8)
            .toList();
    }

    private List<String> mergeRiskFlags(List<String> first, List<String> second) {
        List<String> merged = new ArrayList<>();
        if (first != null) {
            merged.addAll(first);
        }
        if (second != null) {
            merged.addAll(second);
        }
        return merged.stream()
            .filter(item -> item != null && !item.trim().isEmpty())
            .map(String::trim)
            .distinct()
            .limit(12)
            .toList();
    }

    private String defaultText(String text, String fallback) {
        return text == null || text.isBlank() ? fallback : text.trim();
    }

    private void persistPlanEvent(
        LearningSession session,
        PlanMode mode,
        PersonalizedPathContext context,
        PersonalizedPathPlan llmPlan,
        PersonalizedPlanResult appliedResult,
        List<String> schemaErrors,
        List<String> businessErrors,
        String fallbackReason
    ) {
        if (session.getUserPk() == null) {
            return;
        }

        ObjectNode eventData = objectMapper.createObjectNode();
        eventData.put("mode", mode.name());
        eventData.put("source", appliedResult.source().name());
        eventData.put("shadow_mode", appliedResult.shadowMode());
        eventData.put("fallback_applied", appliedResult.fallbackApplied());
        if (fallbackReason != null && !fallbackReason.isBlank()) {
            eventData.put("fallback_reason", fallbackReason);
        }
        eventData.put("prompt_key", appliedResult.promptKey());
        eventData.put("prompt_version", appliedResult.promptVersion());
        eventData.put("provider", appliedResult.provider());
        eventData.put("model", appliedResult.model());
        eventData.put("plan_reasoning_summary", appliedResult.planReasoningSummary());
        eventData.set("risk_flags", objectMapper.valueToTree(appliedResult.riskFlags()));
        eventData.set("context_summary", buildContextSummary(context));
        eventData.set("applied_plan", objectMapper.valueToTree(appliedResult));
        eventData.set("llm_plan", llmPlan == null ? objectMapper.createObjectNode() : objectMapper.valueToTree(llmPlan));

        ObjectNode validation = objectMapper.createObjectNode();
        validation.put("schema_valid", schemaErrors == null || schemaErrors.isEmpty());
        validation.put("business_valid", businessErrors == null || businessErrors.isEmpty());
        validation.set("schema_errors", objectMapper.valueToTree(schemaErrors == null ? List.of() : schemaErrors));
        validation.set("business_errors", objectMapper.valueToTree(businessErrors == null ? List.of() : businessErrors));
        validation.set("merged_errors", objectMapper.valueToTree(appliedResult.validationErrors()));
        eventData.set("validation", validation);

        LearningEvent event = new LearningEvent();
        event.setSessionId(session.getId());
        event.setUserId(session.getUserPk());
        event.setEventType("PATH_PERSONALIZED_PLANNED");
        event.setEventData(eventData.toString());
        learningEventRepository.save(event);
    }

    private ObjectNode buildContextSummary(PersonalizedPathContext context) {
        ObjectNode summary = objectMapper.createObjectNode();
        summary.put("goal_text", defaultText(context.goalText(), ""));
        summary.put("mastery_nodes", context.masteryByNode().size());
        summary.put("recent_error_tags_count", context.recentErrorTags().size());
        summary.put("recent_scores_count", context.recentScores().size());
        summary.put("chapter_node_count", context.chapterNodes().size());
        summary.set("goal_diagnosis", objectMapper.valueToTree(context.goalDiagnosis()));
        return summary;
    }
}
