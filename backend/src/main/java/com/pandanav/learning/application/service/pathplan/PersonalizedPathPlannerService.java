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
import com.pandanav.learning.domain.model.NodeMastery;
import com.pandanav.learning.domain.model.TrainingAttemptSummary;
import com.pandanav.learning.domain.repository.ConceptNodeRepository;
import com.pandanav.learning.domain.repository.LearningEventRepository;
import com.pandanav.learning.domain.repository.MasteryRepository;
import com.pandanav.learning.domain.repository.NodeMasteryRepository;
import com.pandanav.learning.domain.repository.TaskRepository;
import com.pandanav.learning.infrastructure.config.LlmProperties;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
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
    private static final BigDecimal LOW_MASTERY_THRESHOLD = BigDecimal.valueOf(0.6);
    private static final BigDecimal HIGH_MASTERY_THRESHOLD = BigDecimal.valueOf(0.85);
    private static final int LOW_GOAL_DIAGNOSIS_SUM = 55;
    private static final Set<String> BLOCKED_OBJECTIVE_WORDS = Set.of("色情", "仇恨", "暴力", "违法");
    private static final Set<String> PRIORITY_ERROR_TAGS = Set.of("MISSING_STEPS", "CONCEPT_CONFUSION");

    private final ConceptNodeRepository conceptNodeRepository;
    private final MasteryRepository masteryRepository;
    private final NodeMasteryRepository nodeMasteryRepository;
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
        NodeMasteryRepository nodeMasteryRepository,
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
        this.nodeMasteryRepository = nodeMasteryRepository;
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
        Map<Long, Set<String>> errorTagsByNode = buildErrorTagsByNode(taskRepository.findRecentTrainingAttempts(session.getId(), RECENT_ATTEMPT_LIMIT));
        String fallbackReason = null;
        PersonalizedPlanResult ruleApplied = buildRuleResult(
            baseline,
            buildRuleOrderedNodes(baseline, context, errorTagsByNode),
            buildRuleInsertedTasks(baseline, context, errorTagsByNode),
            "Rule planner selected by mode.",
            List.of(),
            shadowMode
        );

        if (mode == PlanMode.RULE) {
            persistPlanEvent(session, mode, context, null, ruleApplied, List.of("mode=rule"), List.of(), fallbackReason);
            return ruleApplied;
        }

        boolean shouldTryLlm = mode == PlanMode.LLM || hasEnoughProfileData(context);
        if (!shouldTryLlm) {
            fallbackReason = "insufficient_profile_data";
            PersonalizedPlanResult result = buildRuleResult(
                baseline,
                ruleApplied.orderedNodes(),
                ruleApplied.insertedTasks(),
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
                ruleApplied.orderedNodes(),
                ruleApplied.insertedTasks(),
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

            List<ConceptNode> llmOrderedNodes = mergeOrderedNodes(parsedPlan.orderedNodes(), chapterNodes);
            List<ConceptNode> orderedNodes = buildRuleOrderedNodes(llmOrderedNodes, context, errorTagsByNode);
            List<PersonalizedPathPlan.InsertedTask> insertedTasks = mergeInsertedTasks(
                parsedPlan.insertedTasks().stream().limit(INSERTED_TASK_LIMIT).toList(),
                buildRuleInsertedTasks(orderedNodes, context, errorTagsByNode)
            );

            PersonalizedPlanResult result = shadowMode
                ? buildRuleResult(
                    baseline,
                    ruleApplied.orderedNodes(),
                    ruleApplied.insertedTasks(),
                    "Shadow mode: LLM plan generated but rule plan is applied.",
                    mergeRiskFlags(parsedPlan.riskFlags(), List.of("SHADOW_MODE")),
                    true
                )
                : new PersonalizedPlanResult(
                    PlanSource.LLM,
                    orderedNodes,
                    insertedTasks,
                    defaultText(parsedPlan.planReasoningSummary(), "LLM personalized plan applied."),
                    sanitizeRiskFlags(mergeRiskFlags(parsedPlan.riskFlags(), deriveRuleRiskFlags(baseline, orderedNodes, insertedTasks, context))),
                    computeAdvancedNodeIds(baseline, orderedNodes),
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
                ruleApplied.orderedNodes(),
                ruleApplied.insertedTasks(),
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
                result.advancedNodeIds(),
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
        String weakPointsSummary = buildWeakPointsSummary(session, masteryByNode, recentErrorTags);

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
            weakPointsSummary,
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
        return nonZeroMastery >= 2
            || context.recentScores().size() >= 2
            || !context.recentErrorTags().isEmpty()
            || (context.weakPointsSummary() != null && !context.weakPointsSummary().isBlank());
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
        List<ConceptNode> orderedNodes,
        List<PersonalizedPathPlan.InsertedTask> insertedTasks,
        String summary,
        List<String> riskFlags,
        boolean shadowMode
    ) {
        List<ConceptNode> appliedOrder = (orderedNodes == null || orderedNodes.isEmpty()) ? baselineNodes : orderedNodes;
        return new PersonalizedPlanResult(
            PlanSource.RULE,
            appliedOrder,
            insertedTasks == null ? List.of() : insertedTasks.stream().limit(INSERTED_TASK_LIMIT).toList(),
            summary,
            sanitizeRiskFlags(riskFlags),
            computeAdvancedNodeIds(baselineNodes, appliedOrder),
            PromptTemplateKey.PATH_PLAN_V1.promptKey(),
            PromptTemplateKey.PATH_PLAN_V1.promptVersion(),
            null,
            null,
            false,
            List.of(),
            shadowMode
        );
    }

    private Map<Long, Set<String>> buildErrorTagsByNode(List<TrainingAttemptSummary> attempts) {
        Map<Long, Set<String>> result = new LinkedHashMap<>();
        for (TrainingAttemptSummary attempt : attempts) {
            if (attempt == null || attempt.nodeId() == null || attempt.errorTags() == null) {
                continue;
            }
            Set<String> tags = result.computeIfAbsent(attempt.nodeId(), key -> new LinkedHashSet<>());
            for (String tag : attempt.errorTags()) {
                if (tag != null && !tag.isBlank()) {
                    tags.add(tag.trim().toUpperCase(Locale.ROOT));
                }
            }
        }
        return result;
    }

    private List<ConceptNode> buildRuleOrderedNodes(
        List<ConceptNode> baselineNodes,
        PersonalizedPathContext context,
        Map<Long, Set<String>> errorTagsByNode
    ) {
        Map<Long, Integer> baselineIndex = new LinkedHashMap<>();
        for (int i = 0; i < baselineNodes.size(); i++) {
            baselineIndex.put(baselineNodes.get(i).getId(), i);
        }
        return baselineNodes.stream()
            .sorted(Comparator
                .<ConceptNode>comparingInt(node -> priorityScore(node, baselineIndex, context, errorTagsByNode))
                .thenComparing(node -> baselineIndex.getOrDefault(node.getId(), Integer.MAX_VALUE))
                .thenComparing(ConceptNode::getId))
            .toList();
    }

    private int priorityScore(
        ConceptNode node,
        Map<Long, Integer> baselineIndex,
        PersonalizedPathContext context,
        Map<Long, Set<String>> errorTagsByNode
    ) {
        int score = baselineIndex.getOrDefault(node.getId(), 0) * 10;
        BigDecimal mastery = context.masteryByNode().get(node.getId());
        if (mastery != null && mastery.compareTo(LOW_MASTERY_THRESHOLD) < 0) {
            score -= 100;
        }
        if (mastery != null && mastery.compareTo(HIGH_MASTERY_THRESHOLD) >= 0) {
            score += 40;
        }
        Set<String> tags = errorTagsByNode.getOrDefault(node.getId(), Set.of());
        if (tags.stream().anyMatch(PRIORITY_ERROR_TAGS::contains)) {
            score -= 80;
        }
        if (tags.size() >= 2) {
            score -= 30;
        }
        return score;
    }

    private List<PersonalizedPathPlan.InsertedTask> buildRuleInsertedTasks(
        List<ConceptNode> orderedNodes,
        PersonalizedPathContext context,
        Map<Long, Set<String>> errorTagsByNode
    ) {
        List<PersonalizedPathPlan.InsertedTask> inserted = new ArrayList<>();
        for (ConceptNode node : orderedNodes) {
            if (inserted.size() >= INSERTED_TASK_LIMIT) {
                break;
            }
            Long nodeId = node.getId();
            Set<String> tags = errorTagsByNode.getOrDefault(nodeId, Set.of());
            BigDecimal mastery = context.masteryByNode().get(nodeId);

            if (tags.stream().anyMatch(PRIORITY_ERROR_TAGS::contains)) {
                String trigger = tags.stream().filter(PRIORITY_ERROR_TAGS::contains).findFirst().orElse("CONCEPT_CONFUSION");
                inserted.add(new PersonalizedPathPlan.InsertedTask(
                    nodeId,
                    Stage.UNDERSTANDING.name(),
                    "针对该节点补齐关键理解断点，并完成错因重建。",
                    trigger
                ));
                continue;
            }
            if (mastery != null && mastery.compareTo(LOW_MASTERY_THRESHOLD) < 0 && !tags.isEmpty()) {
                inserted.add(new PersonalizedPathPlan.InsertedTask(
                    nodeId,
                    Stage.TRAINING.name(),
                    "追加该节点训练强化题，聚焦近期高频错误模式。",
                    "LOW_MASTERY_WITH_ERRORS"
                ));
            }
        }
        if (inserted.size() < INSERTED_TASK_LIMIT && goalDiagnosisTotal(context.goalDiagnosis()) < LOW_GOAL_DIAGNOSIS_SUM && !orderedNodes.isEmpty()) {
            inserted.add(new PersonalizedPathPlan.InsertedTask(
                orderedNodes.get(0).getId(),
                Stage.UNDERSTANDING.name(),
                "先补充目标理解与学习步骤拆解，确保后续训练可执行。",
                "LOW_GOAL_DIAGNOSIS"
            ));
        }
        return inserted.stream().limit(INSERTED_TASK_LIMIT).toList();
    }

    private List<PersonalizedPathPlan.InsertedTask> mergeInsertedTasks(
        List<PersonalizedPathPlan.InsertedTask> llmInserted,
        List<PersonalizedPathPlan.InsertedTask> ruleInserted
    ) {
        List<PersonalizedPathPlan.InsertedTask> merged = new ArrayList<>();
        if (llmInserted != null) {
            merged.addAll(llmInserted.stream().limit(INSERTED_TASK_LIMIT).toList());
        }
        if (ruleInserted != null) {
            for (PersonalizedPathPlan.InsertedTask item : ruleInserted) {
                if (merged.size() >= INSERTED_TASK_LIMIT) {
                    break;
                }
                boolean exists = merged.stream().anyMatch(it ->
                    it.nodeId().equals(item.nodeId())
                        && it.stage().equalsIgnoreCase(item.stage())
                        && it.trigger().equalsIgnoreCase(item.trigger())
                );
                if (!exists) {
                    merged.add(item);
                }
            }
        }
        return merged.stream().limit(INSERTED_TASK_LIMIT).toList();
    }

    private List<Long> computeAdvancedNodeIds(List<ConceptNode> baselineNodes, List<ConceptNode> appliedOrder) {
        Map<Long, Integer> baselineIndex = new LinkedHashMap<>();
        for (int i = 0; i < baselineNodes.size(); i++) {
            baselineIndex.put(baselineNodes.get(i).getId(), i);
        }
        List<Long> advanced = new ArrayList<>();
        for (int i = 0; i < appliedOrder.size(); i++) {
            Integer origin = baselineIndex.get(appliedOrder.get(i).getId());
            if (origin != null && origin > i) {
                advanced.add(appliedOrder.get(i).getId());
            }
        }
        return advanced.stream().distinct().toList();
    }

    private int goalDiagnosisTotal(PersonalizedPathContext.GoalDiagnosisSnapshot diagnosis) {
        if (diagnosis == null) {
            return 0;
        }
        return diagnosis.specificScore()
            + diagnosis.measurableScore()
            + diagnosis.achievableScore()
            + diagnosis.relevantScore()
            + diagnosis.timeBoundScore();
    }

    private List<String> deriveRuleRiskFlags(
        List<ConceptNode> baselineNodes,
        List<ConceptNode> orderedNodes,
        List<PersonalizedPathPlan.InsertedTask> insertedTasks,
        PersonalizedPathContext context
    ) {
        List<String> risk = new ArrayList<>();
        if (!computeAdvancedNodeIds(baselineNodes, orderedNodes).isEmpty()) {
            risk.add("WEAK_NODES_ADVANCED");
        }
        if (insertedTasks != null && !insertedTasks.isEmpty()) {
            risk.add("REMEDIAL_TASK_INSERTED");
        }
        if (context.masteryByNode().values().stream().anyMatch(value -> value != null && value.compareTo(HIGH_MASTERY_THRESHOLD) >= 0)) {
            risk.add("HIGH_MASTERY_DENSITY_REDUCED");
        }
        if (goalDiagnosisTotal(context.goalDiagnosis()) < LOW_GOAL_DIAGNOSIS_SUM) {
            risk.add("LOW_GOAL_DIAGNOSIS");
        }
        return sanitizeRiskFlags(risk);
    }

    private String buildWeakPointsSummary(
        LearningSession session,
        Map<Long, BigDecimal> masteryByNode,
        List<String> recentErrorTags
    ) {
        if (session.getUserPk() != null) {
            List<NodeMastery> rows = nodeMasteryRepository.findByUserIdAndChapterId(session.getUserPk(), session.getChapterId());
            List<String> names = rows.stream()
                .filter(row -> row.getNodeName() != null && !row.getNodeName().isBlank())
                .map(NodeMastery::getNodeName)
                .limit(3)
                .toList();
            if (!names.isEmpty()) {
                return "Weak points detected: " + String.join(", ", names) + ".";
            }
        }

        long lowMasteryCount = masteryByNode.values().stream()
            .filter(value -> value != null && value.compareTo(LOW_MASTERY_THRESHOLD) < 0)
            .count();
        if (lowMasteryCount > 0) {
            return "Low mastery nodes detected: " + lowMasteryCount + ".";
        }

        if (!recentErrorTags.isEmpty()) {
            return "Recent error tags: " + String.join(", ", recentErrorTags.stream().limit(4).toList()) + ".";
        }
        return "No obvious weak point detected for current chapter.";
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
        eventData.set("advanced_node_ids", objectMapper.valueToTree(appliedResult.advancedNodeIds()));
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
        summary.put("weak_points_summary", defaultText(context.weakPointsSummary(), ""));
        summary.put("chapter_node_count", context.chapterNodes().size());
        summary.set("goal_diagnosis", objectMapper.valueToTree(context.goalDiagnosis()));
        return summary;
    }
}

