package com.pandanav.learning.application.service.learningplan;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pandanav.learning.api.dto.CodeLabelDto;
import com.pandanav.learning.api.dto.plan.AdjustLearningPlanResponse;
import com.pandanav.learning.api.dto.plan.ConfirmLearningPlanResponse;
import com.pandanav.learning.api.dto.plan.LearningPlanAdjustmentsDto;
import com.pandanav.learning.api.dto.plan.LearningPlanContextResponse;
import com.pandanav.learning.api.dto.plan.LearningPlanLearnerSnapshotResponse;
import com.pandanav.learning.api.dto.plan.LearningPlanMetadataResponse;
import com.pandanav.learning.api.dto.plan.LearningPlanGuidanceResponse;
import com.pandanav.learning.api.dto.plan.LearningPlanPersonalizationResponse;
import com.pandanav.learning.api.dto.plan.LearningPlanPreviewResponse;
import com.pandanav.learning.api.dto.plan.LearningPlanRecommendationResponse;
import com.pandanav.learning.api.dto.plan.LearningPlanStrategyComparisonResponse;
import com.pandanav.learning.api.dto.plan.LearningPlanStrategyOptionResponse;
import com.pandanav.learning.api.dto.plan.LearningPlanSummaryResponse;
import com.pandanav.learning.api.dto.plan.PlanAlternativeResponse;
import com.pandanav.learning.api.dto.plan.PlanNodeReferenceResponse;
import com.pandanav.learning.api.dto.plan.PlanPathNodeResponse;
import com.pandanav.learning.api.dto.plan.PlanPriorityNodeResponse;
import com.pandanav.learning.api.dto.plan.PlanReasonResponse;
import com.pandanav.learning.api.dto.plan.PlanTaskPreviewResponse;
import com.pandanav.learning.application.command.AdjustLearningPlanCommand;
import com.pandanav.learning.application.command.ConfirmLearningPlanCommand;
import com.pandanav.learning.application.command.PreviewLearningPlanCommand;
import com.pandanav.learning.domain.enums.LearningPlanStatus;
import com.pandanav.learning.domain.enums.SessionStatus;
import com.pandanav.learning.domain.enums.Stage;
import com.pandanav.learning.domain.enums.TaskStatus;
import com.pandanav.learning.domain.model.ConceptNode;
import com.pandanav.learning.domain.model.LearningPlan;
import com.pandanav.learning.domain.model.LearningPlanAggregate;
import com.pandanav.learning.domain.model.LearningPlanContextNode;
import com.pandanav.learning.domain.model.LearningPlanPlanningContext;
import com.pandanav.learning.domain.model.LearningPlanPreview;
import com.pandanav.learning.domain.model.LearningPlanSummary;
import com.pandanav.learning.domain.model.LearnerEvidenceSummary;
import com.pandanav.learning.domain.model.LearnerSignalSnapshot;
import com.pandanav.learning.domain.model.LearnerStateSnapshot;
import com.pandanav.learning.domain.model.LearningSession;
import com.pandanav.learning.domain.model.PlanAdjustments;
import com.pandanav.learning.domain.model.PlanAlternative;
import com.pandanav.learning.domain.model.PlanPathNode;
import com.pandanav.learning.domain.model.PlanReason;
import com.pandanav.learning.domain.model.PlanTaskPreview;
import com.pandanav.learning.domain.model.PersonalizedNarrative;
import com.pandanav.learning.domain.model.PlanGuidance;
import com.pandanav.learning.domain.model.PreviewEnhancement;
import com.pandanav.learning.domain.model.StrategyComparison;
import com.pandanav.learning.domain.model.StrategyOptionComparison;
import com.pandanav.learning.domain.model.Task;
import com.pandanav.learning.domain.policy.TaskObjectiveTemplateStrategy;
import com.pandanav.learning.domain.repository.ConceptNodeRepository;
import com.pandanav.learning.domain.repository.LearningPlanRepository;
import com.pandanav.learning.domain.repository.SessionRepository;
import com.pandanav.learning.domain.repository.TaskRepository;
import com.pandanav.learning.infrastructure.exception.BadRequestException;
import com.pandanav.learning.infrastructure.exception.InternalServerException;
import com.pandanav.learning.infrastructure.exception.NotFoundException;
import com.pandanav.learning.infrastructure.observability.LearningPlanMetricsLogger;
import com.pandanav.learning.infrastructure.observability.TraceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class LearningPlanService {

    private static final Logger log = LoggerFactory.getLogger(LearningPlanService.class);
    private static final String PREVIEW_READY = "PREVIEW_READY";
    private static final String COMMITTED = "COMMITTED";

    private final PlanningContextAssembler planningContextAssembler;
    private final LearningPlanOrchestrator learningPlanOrchestrator;
    private final LearningPlanRepository learningPlanRepository;
    private final PreviewTemplateExplanationAssembler previewTemplateExplanationAssembler;
    private final SessionRepository sessionRepository;
    private final TaskRepository taskRepository;
    private final ConceptNodeRepository conceptNodeRepository;
    private final TaskObjectiveTemplateStrategy taskObjectiveTemplateStrategy;
    private final ObjectMapper objectMapper;
    private final LearnerStateInterpreter learnerStateInterpreter;
    private final LearnerSignalInterpreter learnerSignalInterpreter;
    private final LearnerEvidenceAggregator learnerEvidenceAggregator;
    private final LearningPlanMetricsLogger learningPlanMetricsLogger;
    private final PersonalizedPreviewViewAssembler personalizedPreviewViewAssembler;
    private final PreviewDisplayCodeMapper previewDisplayCodeMapper;

    public LearningPlanService(
        PlanningContextAssembler planningContextAssembler,
        LearningPlanOrchestrator learningPlanOrchestrator,
        LearningPlanRepository learningPlanRepository,
        PreviewTemplateExplanationAssembler previewTemplateExplanationAssembler,
        SessionRepository sessionRepository,
        TaskRepository taskRepository,
        ConceptNodeRepository conceptNodeRepository,
        TaskObjectiveTemplateStrategy taskObjectiveTemplateStrategy,
        ObjectMapper objectMapper,
        LearnerStateInterpreter learnerStateInterpreter,
        LearnerSignalInterpreter learnerSignalInterpreter,
        LearnerEvidenceAggregator learnerEvidenceAggregator,
        LearningPlanMetricsLogger learningPlanMetricsLogger,
        PersonalizedPreviewViewAssembler personalizedPreviewViewAssembler,
        PreviewDisplayCodeMapper previewDisplayCodeMapper
    ) {
        this.planningContextAssembler = planningContextAssembler;
        this.learningPlanOrchestrator = learningPlanOrchestrator;
        this.learningPlanRepository = learningPlanRepository;
        this.previewTemplateExplanationAssembler = previewTemplateExplanationAssembler;
        this.sessionRepository = sessionRepository;
        this.taskRepository = taskRepository;
        this.conceptNodeRepository = conceptNodeRepository;
        this.taskObjectiveTemplateStrategy = taskObjectiveTemplateStrategy;
        this.objectMapper = objectMapper;
        this.learnerStateInterpreter = learnerStateInterpreter;
        this.learnerSignalInterpreter = learnerSignalInterpreter;
        this.learnerEvidenceAggregator = learnerEvidenceAggregator;
        this.learningPlanMetricsLogger = learningPlanMetricsLogger;
        this.personalizedPreviewViewAssembler = personalizedPreviewViewAssembler;
        this.previewDisplayCodeMapper = previewDisplayCodeMapper;
    }

    public LearningPlanPreviewResponse preview(PreviewLearningPlanCommand command) {
        Instant previewStart = Instant.now();
        log.info(
            "LearningPlan preview requested. traceId={} userId={} diagnosisId={} sessionId={}",
            TraceContext.traceId(),
            command.userId(),
            command.diagnosisId(),
            command.sessionId()
        );
        log.info("LearningPlan preview stage=CONTEXT_ASSEMBLE_START traceId={}", TraceContext.traceId());
        LearningPlanPlanningContext context = planningContextAssembler.assemble(command);
        log.info("LearningPlan preview stage=CONTEXT_ASSEMBLE_SUCCESS traceId={} nodeCount={}", TraceContext.traceId(), context.nodes().size());
        LearningPlanOrchestrator.OrchestratedPlan orchestrated = learningPlanOrchestrator.preview(context);
        LearningPlanPlanningContext snapshotContext = enrichContextForSnapshot(context, orchestrated);

        LearningPlan previewDraft = new LearningPlan();
        previewDraft.setUserId(command.userId());
        previewDraft.setGoalId(command.sessionId() == null ? command.goalText() : String.valueOf(command.sessionId()));
        previewDraft.setDiagnosisId(command.diagnosisId());
        previewDraft.setStatus(LearningPlanStatus.DRAFT);
        previewDraft.setLlmTraceId(orchestrated.llmTraceId());
        writeSnapshot(previewDraft, orchestrated.preview(), snapshotContext);

        log.info(
            "LearningPlan preview stage=PERSIST_DRAFT_START traceId={} diagnosisId={} sessionId={}",
            TraceContext.traceId(),
            command.diagnosisId(),
            command.sessionId()
        );
        LearningPlan saved = learningPlanRepository.save(previewDraft);
        log.info("LearningPlan preview stage=PERSIST_DRAFT_SUCCESS traceId={} previewId={}", TraceContext.traceId(), saved.getId());
        LearningPlanPreviewResponse response = toResponse(
            saved,
            orchestrated.preview(),
            snapshotContext,
            orchestrated.planSource(),
            orchestrated.fallbackApplied(),
            orchestrated.fallbackReasons()
        );
        learningPlanMetricsLogger.logPreviewBuilt(
            Boolean.TRUE.equals(orchestrated.fallbackApplied()),
            response.keyEvidence() == null ? 0 : response.keyEvidence().size(),
            response.alternatives() == null ? 0 : response.alternatives().size()
        );
        log.info(
            "LearningPlan preview built. traceId={} planId={} diagnosisId={} sessionId={} recommendedConcept={} strategyCode={} explanationGenerated={} nextActions={} previewLatencyMs={}",
            TraceContext.traceId(),
            response.planId(),
            command.diagnosisId(),
            command.sessionId(),
            response.recommendedEntry() == null ? null : response.recommendedEntry().title(),
            response.recommendedStrategy() == null ? null : response.recommendedStrategy().code(),
            response.explanationGenerated(),
            response.nextActions() == null ? 0 : response.nextActions().size(),
            Duration.between(previewStart, Instant.now()).toMillis()
        );
        return response;
    }

    public AdjustLearningPlanResponse adjust(AdjustLearningPlanCommand command) {
        log.info(
            "LearningPlan adjust requested. traceId={} userId={} previewId={} sessionId={} strategy={} feedback={}",
            TraceContext.traceId(),
            command.userId(),
            command.previewId(),
            command.sessionId(),
            command.strategy(),
            command.userFeedback()
        );
        if (command.previewId() == null && command.sessionId() == null) {
            throw new BadRequestException("previewId or sessionId is required for learning plan adjustment.");
        }

        LearningPlanPreview previousPreview;
        LearningPlanPlanningContext baseContext;
        Long basedOnPreviewId = command.previewId();
        if (command.previewId() != null) {
            LearningPlanAggregate aggregate = load(command.previewId(), command.userId());
            previousPreview = aggregate.preview();
            baseContext = aggregate.planningContext();
        } else {
            LearningSession session = sessionRepository.findByIdAndUserPk(command.sessionId(), command.userId())
                .orElseThrow(() -> new NotFoundException("Learning session not found."));
            baseContext = planningContextAssembler.assemble(new PreviewLearningPlanCommand(
                command.userId(),
                null,
                session.getId(),
                session.getCourseId(),
                session.getChapterId(),
                session.getGoalText(),
                null
            ));
            previousPreview = learningPlanOrchestrator.preview(baseContext).preview();
        }

        LearningPlanPlanningContext adjustedContext = withAdjustmentIntent(baseContext, command, basedOnPreviewId);
        LearningPlanOrchestrator.OrchestratedPlan orchestrated = learningPlanOrchestrator.preview(adjustedContext);
        LearningPlanPlanningContext snapshotContext = enrichContextForSnapshot(adjustedContext, orchestrated);

        LearningPlan previewDraft = new LearningPlan();
        previewDraft.setUserId(command.userId());
        previewDraft.setGoalId(adjustedContext.sourceSessionId() == null ? adjustedContext.goalText() : String.valueOf(adjustedContext.sourceSessionId()));
        previewDraft.setDiagnosisId(adjustedContext.diagnosisId());
        previewDraft.setStatus(LearningPlanStatus.DRAFT);
        previewDraft.setLlmTraceId(orchestrated.llmTraceId());
        writeSnapshot(previewDraft, orchestrated.preview(), snapshotContext);
        LearningPlan saved = learningPlanRepository.save(previewDraft);

        LearningPlanPreviewResponse result = toResponse(
            saved,
            orchestrated.preview(),
            snapshotContext,
            orchestrated.planSource(),
            orchestrated.fallbackApplied(),
            orchestrated.fallbackReasons()
        );
        String oldStrategy = resolveStrategyLabel(baseContext);
        String newStrategy = resolveStrategyLabel(adjustedContext);
        String changeSummary = buildChangeSummary(previousPreview, orchestrated.preview(), oldStrategy, newStrategy);
        String adjustmentReason = buildAdjustmentReason(command);
        log.info(
            "LearningPlan strategy adjusted. previewId={} oldStrategy={} newStrategy={} reason={}",
            command.previewId(),
            oldStrategy,
            newStrategy,
            adjustmentReason
        );
        return new AdjustLearningPlanResponse(result, changeSummary, adjustmentReason);
    }

    public LearningPlanPreviewResponse get(Long previewId, Long userId) {
        LearningPlanAggregate aggregate = load(previewId, userId);
        return toResponse(aggregate.plan(), aggregate.preview(), aggregate.planningContext(), null, null, null);
    }

    public ConfirmLearningPlanResponse confirm(ConfirmLearningPlanCommand command) {
        LearningPlanAggregate aggregate = load(command.previewId(), command.userId());
        LearningPlan previewDraft = aggregate.plan();
        if (previewDraft.getSessionId() != null) {
            return buildConfirmResponse(previewDraft);
        }

        List<ConceptNode> chapterNodes = conceptNodeRepository.findByChapterIdOrderByOrderNoAsc(aggregate.planningContext().chapterId());
        if (chapterNodes.isEmpty()) {
            chapterNodes = bootstrapNodes(aggregate.planningContext());
        }
        Map<String, ConceptNode> nodeByPlanId = alignPlanNodes(aggregate.planningContext().nodes(), chapterNodes);
        String startNodePlanId = aggregate.preview().summary().recommendedStartNodeId();
        ConceptNode startNode = nodeByPlanId.get(startNodePlanId);
        if (startNode == null) {
            throw new InternalServerException("Learning plan start node is missing.");
        }

        LearningSession session = new LearningSession();
        session.setUserId("user-" + command.userId());
        session.setUserPk(command.userId());
        session.setCourseId(aggregate.planningContext().courseId());
        session.setChapterId(aggregate.planningContext().chapterId());
        session.setGoalText(aggregate.planningContext().goalText());
        session.setCurrentNodeId(startNode.getId());
        session.setCurrentStage(Stage.STRUCTURE);
        session.setStatus(SessionStatus.PLANNING);
        LearningSession savedSession = sessionRepository.save(session);

        List<Task> tasks = new ArrayList<>();
        for (PlanPathNode pathNode : aggregate.preview().pathPreview()) {
            ConceptNode conceptNode = nodeByPlanId.get(pathNode.nodeId());
            if (conceptNode == null) {
                continue;
            }
            tasks.add(buildTask(savedSession.getId(), conceptNode.getId(), Stage.STRUCTURE, conceptNode.getName()));
            tasks.add(buildTask(savedSession.getId(), conceptNode.getId(), Stage.UNDERSTANDING, conceptNode.getName()));
            tasks.add(buildTask(savedSession.getId(), conceptNode.getId(), Stage.TRAINING, conceptNode.getName()));
            tasks.add(buildTask(savedSession.getId(), conceptNode.getId(), Stage.REFLECTION, conceptNode.getName()));
        }
        List<Task> savedTasks = taskRepository.saveAll(tasks);
        sessionRepository.updateStatus(savedSession.getId(), SessionStatus.LEARNING);

        previewDraft.setSessionId(savedSession.getId());
        previewDraft.setStatus(LearningPlanStatus.CONFIRMED);
        learningPlanRepository.update(previewDraft);

        Long firstTaskId = savedTasks.stream().findFirst().map(Task::getId).orElse(null);
        learningPlanMetricsLogger.logPreviewAccepted(firstTaskId != null);
        return new ConfirmLearningPlanResponse(
            String.valueOf(previewDraft.getId()),
            savedSession.getId(),
            startNode.getId(),
            firstTaskId,
            "/sessions/" + savedSession.getId()
        );
    }

    private ConfirmLearningPlanResponse buildConfirmResponse(LearningPlan plan) {
        List<Task> tasks = taskRepository.findBySessionIdWithStatus(plan.getSessionId());
        Long firstTaskId = tasks.isEmpty() ? null : tasks.get(0).getId();
        learningPlanMetricsLogger.logPreviewAccepted(firstTaskId != null);
        Long currentNodeId = sessionRepository.findByIdAndUserPk(plan.getSessionId(), plan.getUserId())
            .map(LearningSession::getCurrentNodeId)
            .orElse(null);
        return new ConfirmLearningPlanResponse(
            String.valueOf(plan.getId()),
            plan.getSessionId(),
            currentNodeId,
            firstTaskId,
            "/sessions/" + plan.getSessionId()
        );
    }

    private LearningPlanAggregate load(Long previewId, Long userId) {
        LearningPlan plan = learningPlanRepository.findByIdAndUserId(previewId, userId)
            .orElseThrow(() -> new NotFoundException("Learning plan not found."));
        try {
            LearningPlanPreview preview = new LearningPlanPreview(
                objectMapper.readValue(plan.getSummaryJson(), LearningPlanSummary.class),
                objectMapper.readValue(plan.getReasonsJson(), new TypeReference<List<PlanReason>>() { }),
                objectMapper.readValue(plan.getFocusesJson(), new TypeReference<List<String>>() { }),
                objectMapper.readValue(plan.getPathPreviewJson(), new TypeReference<List<PlanPathNode>>() { }),
                objectMapper.readValue(plan.getTaskPreviewJson(), new TypeReference<List<PlanTaskPreview>>() { }),
                objectMapper.readValue(plan.getAdjustmentsJson(), PlanAdjustments.class)
            );
            LearningPlanPlanningContext context = objectMapper.readValue(plan.getPlanningContextJson(), LearningPlanPlanningContext.class);
            return new LearningPlanAggregate(plan, preview, context);
        } catch (Exception ex) {
            throw new InternalServerException("Failed to read stored learning plan.");
        }
    }

    private void writeSnapshot(LearningPlan plan, LearningPlanPreview preview, LearningPlanPlanningContext context) {
        try {
            plan.setSummaryJson(objectMapper.writeValueAsString(preview.summary()));
            plan.setReasonsJson(objectMapper.writeValueAsString(preview.reasons()));
            plan.setFocusesJson(objectMapper.writeValueAsString(preview.focuses()));
            plan.setPathPreviewJson(objectMapper.writeValueAsString(preview.pathPreview()));
            plan.setTaskPreviewJson(objectMapper.writeValueAsString(preview.taskPreview()));
            plan.setAdjustmentsJson(objectMapper.writeValueAsString(preview.adjustments()));
            plan.setPlanningContextJson(objectMapper.writeValueAsString(context));
        } catch (Exception ex) {
            throw new InternalServerException("Failed to serialize learning plan.");
        }
    }

    private LearningPlanPreviewResponse toResponse(
        LearningPlan plan,
        LearningPlanPreview preview,
        LearningPlanPlanningContext context,
        PlanSource planContentSource,
        Boolean fallbackApplied,
        List<String> fallbackReasons
    ) {
        boolean committed = plan.getStatus() == LearningPlanStatus.CONFIRMED || plan.getSessionId() != null;
        LearningPlanSummary summary = preview.summary();
        PreviewTemplateExplanationAssembler.PreviewExplanations explanations =
            previewTemplateExplanationAssembler.build(preview, context);
        LearnerEvidenceSummary evidenceSummary = context == null ? null : context.learnerEvidenceSummary();
        log.info(
            "LearningPlan preview explanation. traceId={} diagnosisId={} sessionId={} planId={} llmLatencyMs={} promptTokens={} completionTokens={} totalTokens={} templateFallback={} explanationGenerated={}",
            TraceContext.traceId(),
            context == null ? null : context.diagnosisId(),
            context == null ? null : context.sourceSessionId(),
            plan.getId(),
            explanations.llmLatencyMs(),
            explanations.promptTokens(),
            explanations.completionTokens(),
            explanations.totalTokens(),
            explanations.templateFallback(),
            explanations.explanationGenerated()
        );
        List<String> nextActions = buildNextActions(preview);
        List<String> profileDrivenReasoning = buildProfileDrivenReasoning(context);
        List<String> mergedEvidence = mergeEvidence(
            evidenceSummary == null ? explanations.learnerEvidence() : evidenceSummary.topEvidence(),
            profileDrivenReasoning
        );
        List<String> profileConflicts = buildProfileConflicts(context);
        List<String> riskFlags = buildRiskFlags(profileConflicts);
        String whyThisStep = mergeWhyThisStep(
            evidenceSummary == null ? explanations.recommendedEntryReason() : evidenceSummary.whyThisStep(),
            profileDrivenReasoning
        );
        PersonalizedPreviewViewAssembler.PersonalizedPreviewView displayView = personalizedPreviewViewAssembler.assemble(
            context,
            preview,
            whyThisStep,
            profileDrivenReasoning,
            evidenceSummary == null ? "完成这一步后，你会更容易进入后续训练并减少回退。" : evidenceSummary.expectedGain()
        );
        String finalStrategyCode = resolveFinalStrategyCode(summary, explanations);
        return new LearningPlanPreviewResponse(
            String.valueOf(plan.getId()),
            committed ? COMMITTED : PREVIEW_READY,
            !committed,
            committed,
            context == null ? null : context.goalText(),
            new LearningPlanPreviewResponse.RecommendedEntryResponse(
                summary.recommendedStartNodeId(),
                summary.recommendedStartNodeName(),
                resolveEstimatedMinutes(summary),
                explanations.recommendedEntryReason()
            ),
            new LearningPlanPreviewResponse.LearnerSnapshotResponse(
                explanations.learnerCurrentState(),
                explanations.learnerEvidence()
            ),
            new LearningPlanPreviewResponse.RecommendedStrategyResponse(
                finalStrategyCode,
                previewTemplateExplanationAssembler.strategyLabel(finalStrategyCode),
                explanations.strategyExplanation()
            ),
            explanations.alternatives(),
            nextActions,
            whyThisStep,
            mergedEvidence,
            profileDrivenReasoning,
            evidenceSummary == null ? resolveRiskIfSkipped(preview.reasons(), summary) : evidenceSummary.skipRisk(),
            evidenceSummary == null ? "完成这一步后，你会更容易进入后续训练并减少回退。" : evidenceSummary.expectedGain(),
            evidenceSummary == null ? resolveConfidenceExplanation(context, null) : evidenceSummary.confidenceHint(),
            riskFlags,
            profileConflicts,
            new LearningPlanAdjustmentsDto(
                preview.adjustments().intensity(),
                preview.adjustments().learningMode(),
                preview.adjustments().preferPrerequisite()
            ),
            committed
                ? "已进入正式学习流程，系统会直接把你带到第一步任务。"
                : "确认后系统会创建正式计划，并带你进入第一步任务。",
            explanations.explanationGenerated(),
            displayView.personalizedSummary(),
            displayView.currentTaskCard(),
            displayView.personalizedReasons(),
            displayView.explanationPanel(),
            plan.getCreatedAt() == null ? OffsetDateTime.now() : plan.getCreatedAt(),
            TraceContext.traceId()
        );
    }

    private String resolveFinalStrategyCode(
        LearningPlanSummary summary,
        PreviewTemplateExplanationAssembler.PreviewExplanations explanations
    ) {
        if (summary != null && summary.selectedStrategyCode() != null && !summary.selectedStrategyCode().isBlank()) {
            return summary.selectedStrategyCode().trim().toUpperCase(Locale.ROOT);
        }
        return explanations.strategyCode();
    }

    private String mergeWhyThisStep(String base, List<String> profileDrivenReasoning) {
        String normalizedBase = base == null ? null : base.trim();
        if (profileDrivenReasoning == null || profileDrivenReasoning.isEmpty()) {
            return normalizedBase;
        }
        return (normalizedBase == null || normalizedBase.isBlank())
            ? profileDrivenReasoning.get(0)
            : normalizedBase + " " + profileDrivenReasoning.get(0);
    }

    private List<String> mergeEvidence(List<String> base, List<String> profileDrivenReasoning) {
        List<String> merged = new ArrayList<>();
        if (base != null) {
            merged.addAll(base.stream()
                .filter(item -> item != null && !item.isBlank())
                .map(String::trim)
                .toList());
        }
        if (profileDrivenReasoning != null) {
            merged.addAll(profileDrivenReasoning.stream()
                .filter(item -> item != null && !item.isBlank())
                .map(String::trim)
                .toList());
        }
        if (merged.isEmpty()) {
            return List.of();
        }
        return merged.stream().distinct().limit(4).toList();
    }

    private List<String> buildProfileDrivenReasoning(LearningPlanPlanningContext context) {
        if (context == null || context.learnerProfileSnapshot() == null) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        String learningPreference = readSnapshotValue(context, true, "learningPreference");
        String supportPriority = readSnapshotValue(context, true, "supportPriority");
        String timeBudget = readSnapshotValue(context, false, "timeBudget");
        if (!learningPreference.isBlank()) {
            result.add("你更适合「" + previewDisplayCodeMapper.learningPreference(learningPreference) + "」，本轮已按这个方式安排。");
        }
        if (!supportPriority.isBlank()) {
            result.add("画像显示当前支持优先级为「" + supportPriority + "」，因此优先安排对应补齐动作。");
        }
        if (result.size() < 2 && !timeBudget.isBlank()) {
            result.add("你当前可投入「" + previewDisplayCodeMapper.timeBudget(timeBudget) + "」，当前节奏已做匹配。");
        }
        return result.stream().limit(2).toList();
    }

    private List<String> buildRiskFlags(List<String> profileConflicts) {
        if (profileConflicts == null || profileConflicts.isEmpty()) {
            return List.of();
        }
        List<String> flags = new ArrayList<>();
        if (profileConflicts.stream().anyMatch(item -> item.contains("OVERCONFIDENCE"))) {
            flags.add("OVERCONFIDENCE_RISK");
        }
        return flags.stream().distinct().toList();
    }

    private List<String> buildProfileConflicts(LearningPlanPlanningContext context) {
        if (context == null || context.learnerProfileSnapshot() == null) {
            return List.of();
        }
        List<String> conflicts = new ArrayList<>();
        String foundationLevel = readFeatureValue(context, "foundation_level");
        String practiceExperience = readFeatureValue(context, "practice_experience");
        boolean selfRatedHigh = "PROFICIENT".equals(foundationLevel) || "ADVANCED".equals(foundationLevel);
        boolean weakExperience = "NONE".equals(practiceExperience) || "COURSEWORK".equals(practiceExperience);
        boolean weakRuntimeSignal = context.learnerSignalSnapshot() != null
            && (isWeakTier(context.learnerSignalSnapshot().conceptUnderstanding())
            || isWeakTier(context.learnerSignalSnapshot().relationshipUnderstanding())
            || isWeakTier(context.learnerSignalSnapshot().codeMapping()));
        if (selfRatedHigh && (weakExperience || weakRuntimeSignal)) {
            conflicts.add("OVERCONFIDENCE_PROFILE_CONFLICT");
        }
        return conflicts.stream().distinct().toList();
    }

    private boolean isWeakTier(com.pandanav.learning.domain.enums.LearnerSignalTier tier) {
        return tier == com.pandanav.learning.domain.enums.LearnerSignalTier.WEAK;
    }

    private String readSnapshotValue(LearningPlanPlanningContext context, boolean strategyHints, String key) {
        if (context == null || context.learnerProfileSnapshot() == null || key == null || key.isBlank()) {
            return "";
        }
        Map<String, Object> source = strategyHints
            ? context.learnerProfileSnapshot().getStrategyHints()
            : context.learnerProfileSnapshot().getConstraints();
        if (source == null || source.isEmpty()) {
            return "";
        }
        Object value = source.get(key);
        if (value == null) {
            return "";
        }
        return String.valueOf(value).trim().toUpperCase(Locale.ROOT);
    }

    private String readFeatureValue(LearningPlanPlanningContext context, String featureKey) {
        if (context == null
            || context.learnerProfileSnapshot() == null
            || context.learnerProfileSnapshot().getFeatureSummary() == null
            || featureKey == null
            || featureKey.isBlank()) {
            return "";
        }
        Object features = context.learnerProfileSnapshot().getFeatureSummary().get("features");
        if (!(features instanceof List<?> list)) {
            return "";
        }
        for (Object item : list) {
            if (!(item instanceof Map<?, ?> map)) {
                continue;
            }
            Object key = map.get("featureKey");
            if (key == null || !featureKey.equalsIgnoreCase(String.valueOf(key).trim())) {
                continue;
            }
            Object value = map.get("featureValue");
            if (value != null) {
                return String.valueOf(value).trim().toUpperCase(Locale.ROOT);
            }
        }
        return "";
    }

    private Integer resolveEstimatedMinutes(LearningPlanSummary summary) {
        if (summary.taskEstimatedMinutes() != null && summary.taskEstimatedMinutes() > 0) {
            return summary.taskEstimatedMinutes();
        }
        if (summary.estimatedMinutes() == null || summary.estimatedMinutes() <= 0) {
            return 15;
        }
        return Math.max(6, summary.estimatedMinutes() / Math.max(summary.estimatedStageCount() == null ? 1 : summary.estimatedStageCount(), 1));
    }

    private List<String> buildNextActions(LearningPlanPreview preview) {
        List<String> actions = new ArrayList<>();
        if (preview.taskPreview() != null) {
            for (PlanTaskPreview item : preview.taskPreview()) {
                if (item == null || item.title() == null || item.title().isBlank()) {
                    continue;
                }
                actions.add(item.title().trim());
                if (actions.size() >= 3) {
                    break;
                }
            }
        }
        while (actions.size() < 3) {
            actions.add(switch (actions.size()) {
                case 0 -> "先完成第一步的结构梳理";
                case 1 -> "再做一次关键理解检查";
                default -> "最后用一轮短练习确认掌握";
            });
        }
        return actions;
    }

    private LearningPlanPlanningContext withAdjustmentIntent(
        LearningPlanPlanningContext baseContext,
        AdjustLearningPlanCommand command,
        Long basedOnPreviewId
    ) {
        PlanAdjustments adjustments = resolveAdjustments(baseContext.adjustments(), command.strategy(), command.timeBudget());
        LearningPlanPlanningContext adjusted = new LearningPlanPlanningContext(
            baseContext.userId(),
            baseContext.goalId(),
            baseContext.diagnosisId(),
            baseContext.courseId(),
            baseContext.chapterId(),
            baseContext.goalText(),
            command.sessionId() != null ? command.sessionId() : baseContext.sourceSessionId(),
            baseContext.nodes(),
            baseContext.recentErrorTags(),
            baseContext.recentScores(),
            baseContext.weakPointLabels(),
            baseContext.learnerProfileSummary(),
            adjustments,
            normalizeStrategy(command.strategy()),
            command.timeBudget(),
            command.reason(),
            normalizeFeedback(command.userFeedback()),
            basedOnPreviewId,
            baseContext.learnerProfileSnapshot(),
            null,
            null,
            null,
            null,
            null
        );
        LearnerSignalSnapshot learnerSignalSnapshot = learnerSignalInterpreter.interpret(adjusted);
        return new LearningPlanPlanningContext(
            adjusted.userId(),
            adjusted.goalId(),
            adjusted.diagnosisId(),
            adjusted.courseId(),
            adjusted.chapterId(),
            adjusted.goalText(),
            adjusted.sourceSessionId(),
            adjusted.nodes(),
            adjusted.recentErrorTags(),
            adjusted.recentScores(),
            adjusted.weakPointLabels(),
            adjusted.learnerProfileSummary(),
            adjusted.adjustments(),
            adjusted.requestedStrategy(),
            adjusted.requestedTimeBudgetMinutes(),
            adjusted.adjustmentReason(),
            adjusted.userFeedback(),
            adjusted.basedOnPreviewId(),
            adjusted.learnerProfileSnapshot(),
            learnerStateInterpreter.interpret(adjusted),
            learnerSignalSnapshot,
            learnerEvidenceAggregator.aggregate(adjusted, learnerSignalSnapshot, null),
            null,
            null
        );
    }

    private LearningPlanPlanningContext enrichContextForSnapshot(
        LearningPlanPlanningContext context,
        LearningPlanOrchestrator.OrchestratedPlan orchestrated
    ) {
        LearnerStateSnapshot learnerState = orchestrated.learnerStateSnapshot() != null
            ? orchestrated.learnerStateSnapshot()
            : (context.learnerStateSnapshot() != null ? context.learnerStateSnapshot() : learnerStateInterpreter.interpret(context));
        LearnerSignalSnapshot learnerSignal = context.learnerSignalSnapshot() != null
            ? context.learnerSignalSnapshot()
            : learnerSignalInterpreter.interpret(context);
        String recommendedNodeName = orchestrated.preview() == null || orchestrated.preview().summary() == null
            ? null
            : orchestrated.preview().summary().recommendedStartNodeName();
        LearnerEvidenceSummary learnerEvidence = learnerEvidenceAggregator.aggregate(context, learnerSignal, recommendedNodeName);
        return new LearningPlanPlanningContext(
            context.userId(),
            context.goalId(),
            context.diagnosisId(),
            context.courseId(),
            context.chapterId(),
            context.goalText(),
            context.sourceSessionId(),
            context.nodes(),
            context.recentErrorTags(),
            context.recentScores(),
            context.weakPointLabels(),
            context.learnerProfileSummary(),
            context.adjustments(),
            context.requestedStrategy(),
            context.requestedTimeBudgetMinutes(),
            context.adjustmentReason(),
            context.userFeedback(),
            context.basedOnPreviewId(),
            context.learnerProfileSnapshot(),
            learnerState,
            learnerSignal,
            learnerEvidence,
            orchestrated.personalizedNarrative(),
            orchestrated.previewEnhancement()
        );
    }

    private LearningPlanPersonalizationResponse mapPersonalization(LearningPlanPlanningContext context) {
        if (context == null || context.personalizedNarrative() == null) {
            return null;
        }
        PersonalizedNarrative narrative = context.personalizedNarrative();
        LearnerStateSnapshot snapshot = context.learnerStateSnapshot();
        return new LearningPlanPersonalizationResponse(
            narrative.learnerState(),
            narrative.whatISaw() == null ? List.of() : narrative.whatISaw(),
            narrative.whyThisPlanFitsYou(),
            narrative.mainRiskIfSkip(),
            narrative.thisRoundBoundary(),
            narrative.adaptationHint(),
            snapshot == null ? null : snapshot.confidenceReasonSummary(),
            snapshot == null ? null : snapshot.motivationRisk().name()
        );
    }

    private LearningPlanGuidanceResponse mapPlanGuidance(PreviewEnhancement enhancement) {
        if (enhancement == null || enhancement.planGuidance() == null) {
            return null;
        }
        PlanGuidance guidance = enhancement.planGuidance();
        return new LearningPlanGuidanceResponse(
            guidance.whyChosen(),
            guidance.whyNotAlternatives(),
            guidance.learnerMirror(),
            guidance.firstAction(),
            guidance.firstCheckpoint(),
            guidance.planTradeoff(),
            guidance.ifPerformWell(),
            guidance.ifStillStruggle(),
            guidance.ifNoTime(),
            guidance.startPrompt(),
            guidance.kickoffSteps() == null ? List.of() : guidance.kickoffSteps(),
            guidance.warmupGoal(),
            guidance.validationFocus(),
            guidance.evidenceMode(),
            guidance.adaptationPolicy(),
            guidance.confidenceExplanation()
        );
    }

    private LearningPlanStrategyComparisonResponse mapStrategyComparison(PreviewEnhancement enhancement) {
        if (enhancement == null || enhancement.strategyComparison() == null) {
            return null;
        }
        StrategyComparison strategyComparison = enhancement.strategyComparison();
        return new LearningPlanStrategyComparisonResponse(
            strategyComparison.currentRecommendedStrategy(),
            strategyComparison.options() == null
                ? List.of()
                : strategyComparison.options().stream()
                .map(this::mapStrategyOption)
                .toList()
        );
    }

    private LearningPlanStrategyOptionResponse mapStrategyOption(StrategyOptionComparison item) {
        return new LearningPlanStrategyOptionResponse(
            item.strategy(),
            item.label(),
            item.suitableFor(),
            item.notIdealWhen(),
            item.switchingCostRisk()
        );
    }

    private String resolveConfidenceExplanation(
        LearningPlanPlanningContext context,
        LearningPlanGuidanceResponse guidance
    ) {
        if (guidance != null && guidance.confidenceExplanation() != null && !guidance.confidenceExplanation().isBlank()) {
            return guidance.confidenceExplanation();
        }
        if (context == null || context.learnerStateSnapshot() == null) {
            return null;
        }
        return context.learnerStateSnapshot().confidenceReasonSummary();
    }

    private PlanAdjustments resolveAdjustments(PlanAdjustments base, String strategy, Integer timeBudget) {
        String normalizedStrategy = normalizeStrategy(strategy);
        PlanAdjustments adjusted = switch (normalizedStrategy) {
            case "FAST_TRACK" -> new PlanAdjustments("INTENSIVE", "MIXED", false);
            case "PRACTICE_FIRST" -> new PlanAdjustments("STANDARD", "PRACTICE_DRIVEN", false);
            case "COMPRESSED_10_MIN" -> new PlanAdjustments("LIGHT", "MIXED", false);
            default -> new PlanAdjustments("STANDARD", "LEARN_THEN_PRACTICE", true);
        };
        if (timeBudget != null && timeBudget > 0 && timeBudget <= 10) {
            adjusted = new PlanAdjustments("LIGHT", adjusted.learningMode(), adjusted.preferPrerequisite());
        }
        if (base == null) {
            return adjusted.normalized();
        }
        return new PlanAdjustments(adjusted.intensity(), adjusted.learningMode(), adjusted.preferPrerequisite()).normalized();
    }

    private String normalizeStrategy(String value) {
        if (value == null || value.isBlank()) {
            throw new BadRequestException("strategy is required.");
        }
        String normalized = value.trim().toUpperCase();
        return switch (normalized) {
            case "FAST_TRACK", "FOUNDATION_FIRST", "PRACTICE_FIRST", "COMPRESSED_10_MIN" -> normalized;
            default -> throw new BadRequestException("Unsupported strategy: " + value);
        };
    }

    private String normalizeFeedback(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase();
        return switch (normalized) {
            case "I_ALREADY_KNOW_THIS", "TOO_SLOW", "TIME_LIMITED", "DIDNT_UNDERSTAND_WHY" -> normalized;
            default -> throw new BadRequestException("Unsupported userFeedback: " + value);
        };
    }

    private String buildChangeSummary(LearningPlanPreview oldPreview, LearningPlanPreview newPreview, String oldStrategy, String newStrategy) {
        return "策略从 " + oldStrategy + " 调整为 " + newStrategy
            + "，学习焦点从 " + safe(oldPreview.summary().currentFocusLabel()) + " 调整为 " + safe(newPreview.summary().currentFocusLabel())
            + "，当前步骤从 " + safe(oldPreview.summary().taskTitle()) + " 调整为 " + safe(newPreview.summary().taskTitle()) + "。";
    }

    private String buildAdjustmentReason(AdjustLearningPlanCommand command) {
        List<String> parts = new ArrayList<>();
        parts.add("已按 " + normalizeStrategy(command.strategy()) + " 重新规划");
        if (command.timeBudget() != null) {
            parts.add("时间预算 " + command.timeBudget() + " 分钟");
        }
        if (command.userFeedback() != null && !command.userFeedback().isBlank()) {
            parts.add("反馈为 " + normalizeFeedback(command.userFeedback()));
        }
        if (command.reason() != null && !command.reason().isBlank()) {
            parts.add("原因：" + command.reason().trim());
        }
        return String.join("，", parts) + "。";
    }

    private String resolveStrategyLabel(LearningPlanPlanningContext context) {
        if (context.requestedStrategy() != null && !context.requestedStrategy().isBlank()) {
            return context.requestedStrategy();
        }
        if ("PRACTICE_DRIVEN".equalsIgnoreCase(context.adjustments().learningMode())) {
            return "PRACTICE_FIRST";
        }
        if ("LIGHT".equalsIgnoreCase(context.adjustments().intensity())) {
            return "COMPRESSED_10_MIN";
        }
        if (!context.adjustments().preferPrerequisite()) {
            return "FAST_TRACK";
        }
        return "FOUNDATION_FIRST";
    }

    private List<PlanReasonResponse> mapReasons(List<PlanReason> reasons) {
        if (reasons == null) {
            return List.of();
        }
        return reasons.stream()
            .map(item -> new PlanReasonResponse(item.type(), item.title(), item.description()))
            .toList();
    }

    private List<PlanAlternativeResponse> mapAlternatives(List<PlanAlternative> alternatives) {
        if (alternatives == null) {
            return List.of();
        }
        return alternatives.stream()
            .map(item -> new PlanAlternativeResponse(item.strategy(), item.label(), item.description(), item.tradeoff()))
            .toList();
    }

    private Integer resolveMasteryScore(LearningPlanPlanningContext context, String startNodeId) {
        if (context == null || context.nodes() == null || context.nodes().isEmpty()) {
            return null;
        }
        return context.nodes().stream()
            .filter(node -> startNodeId == null || startNodeId.equals(node.planNodeId()))
            .map(LearningPlanContextNode::mastery)
            .findFirst()
            .orElseGet(() -> {
                int sum = context.nodes().stream().mapToInt(LearningPlanContextNode::mastery).sum();
                return sum / context.nodes().size();
            });
    }

    private String resolveRiskIfSkipped(List<PlanReason> reasons, LearningPlanSummary summary) {
        if (reasons != null) {
            Optional<String> risk = reasons.stream()
                .filter(item -> "RISK_CONTROL".equalsIgnoreCase(item.type()))
                .map(PlanReason::description)
                .filter(text -> text != null && !text.isBlank())
                .findFirst();
            if (risk.isPresent()) {
                return risk.get();
            }
        }
        return summary.whyNow();
    }

    private CodeLabelDto toContentSource(String contentSourceType) {
        if ("LLM".equalsIgnoreCase(contentSourceType)) {
            return new CodeLabelDto("LLM", "LLM");
        }
        return new CodeLabelDto("FALLBACK", "Fallback");
    }

    private String resolveDifficultyCode(Integer difficulty) {
        int value = difficulty == null ? 2 : difficulty;
        if (value <= 1) {
            return "FOUNDATION";
        }
        if (value >= 4) {
            return "CHALLENGE";
        }
        return "CORE";
    }

    private String resolvePathStatus(Integer mastery) {
        if (mastery == null) {
            return "NEW";
        }
        if (mastery < 50) {
            return "WEAK";
        }
        if (mastery < 80) {
            return "PARTIAL";
        }
        return "STABLE";
    }

    private List<ConceptNode> bootstrapNodes(LearningPlanPlanningContext context) {
        List<ConceptNode> saved = new ArrayList<>();
        for (LearningPlanContextNode node : context.nodes()) {
            ConceptNode conceptNode = new ConceptNode();
            conceptNode.setChapterId(context.chapterId());
            conceptNode.setName(node.nodeName());
            conceptNode.setOutline("Goal=" + context.goalText());
            conceptNode.setOrderNo(node.orderNo());
            saved.add(conceptNodeRepository.save(conceptNode));
        }
        return saved;
    }

    private Map<String, ConceptNode> alignPlanNodes(List<LearningPlanContextNode> plannedNodes, List<ConceptNode> persistedNodes) {
        List<ConceptNode> orderedPersisted = persistedNodes.stream()
            .sorted(Comparator.comparing(ConceptNode::getOrderNo).thenComparing(ConceptNode::getId))
            .toList();
        Map<Long, ConceptNode> byId = orderedPersisted.stream()
            .collect(Collectors.toMap(ConceptNode::getId, node -> node, (left, right) -> left, LinkedHashMap::new));

        Map<String, ConceptNode> result = new LinkedHashMap<>();
        for (LearningPlanContextNode node : plannedNodes) {
            if (node.persistedNodeId() != null && byId.containsKey(node.persistedNodeId())) {
                result.put(node.planNodeId(), byId.get(node.persistedNodeId()));
            }
        }
        for (int i = 0; i < Math.min(plannedNodes.size(), orderedPersisted.size()); i++) {
            result.putIfAbsent(plannedNodes.get(i).planNodeId(), orderedPersisted.get(i));
        }
        return result;
    }

    private Task buildTask(Long sessionId, Long nodeId, Stage stage, String conceptName) {
        Task task = new Task();
        task.setSessionId(sessionId);
        task.setNodeId(nodeId);
        task.setStage(stage);
        task.setStatus(TaskStatus.PENDING);
        task.setObjective(taskObjectiveTemplateStrategy.buildObjective(stage, conceptName));
        return task;
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "未命名" : value.trim();
    }
}
