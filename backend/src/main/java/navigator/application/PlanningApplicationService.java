package navigator.application;

import navigator.api.BusinessErrorCode;
import navigator.api.BusinessException;
import navigator.api.dto.CommitPlanData;
import navigator.api.dto.PlanPreviewData;
import navigator.application.guard.EntityLookupGuard;
import navigator.application.guard.SessionStateGuard;
import navigator.application.planning.PlanStrategySelector;
import navigator.application.planning.PlanTemplateFactory;
import navigator.application.planning.PlanningContext;
import navigator.application.planning.PlanningContextAssembler;
import navigator.application.planning.RecommendedEntryBuilder;
import navigator.domain.enums.PlanStatus;
import navigator.domain.enums.RecommendedStrategyCode;
import navigator.domain.model.ExecutableTaskSpec;
import navigator.domain.model.LearningPlanPreview;
import navigator.domain.model.RecommendedEntry;
import navigator.domain.model.RecommendedStrategy;
import navigator.domain.model.TaskBlueprint;
import navigator.domain.model.TimeBudgetConstraint;
import navigator.application.task.TaskSpecFromBlueprintConverter;
import navigator.infrastructure.memory.InMemoryStore;
import navigator.infrastructure.persistence.entity.LearningPlanEntity;
import navigator.infrastructure.persistence.entity.SessionTaskEntity;
import navigator.infrastructure.persistence.repository.LearningPlanRepository;
import navigator.infrastructure.persistence.repository.LearningSessionRepository;
import navigator.infrastructure.persistence.repository.SessionTaskRepository;
import navigator.infrastructure.persistence.serde.JsonSerde;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class PlanningApplicationService {

    private final InMemoryStore store;
    private final EntityLookupGuard entityLookupGuard;
    private final SessionStateGuard sessionStateGuard;
    private final PlanningContextAssembler planningContextAssembler;
    private final PlanStrategySelector planStrategySelector;
    private final RecommendedEntryBuilder recommendedEntryBuilder;
    private final PlanTemplateFactory planTemplateFactory;
    private final LearningPlanRepository learningPlanRepository;
    private final SessionTaskRepository sessionTaskRepository;
    private final LearningSessionRepository learningSessionRepository;
    private final JsonSerde jsonSerde;
    private final TaskSpecFromBlueprintConverter taskSpecConverter;

    public PlanningApplicationService(InMemoryStore store,
                                      EntityLookupGuard entityLookupGuard,
                                      SessionStateGuard sessionStateGuard,
                                      PlanningContextAssembler planningContextAssembler,
                                      PlanStrategySelector planStrategySelector,
                                      RecommendedEntryBuilder recommendedEntryBuilder,
                                      PlanTemplateFactory planTemplateFactory,
                                      LearningPlanRepository learningPlanRepository,
                                      SessionTaskRepository sessionTaskRepository,
                                      LearningSessionRepository learningSessionRepository,
                                      JsonSerde jsonSerde,
                                      TaskSpecFromBlueprintConverter taskSpecConverter) {
        this.store = store;
        this.entityLookupGuard = entityLookupGuard;
        this.sessionStateGuard = sessionStateGuard;
        this.planningContextAssembler = planningContextAssembler;
        this.planStrategySelector = planStrategySelector;
        this.recommendedEntryBuilder = recommendedEntryBuilder;
        this.planTemplateFactory = planTemplateFactory;
        this.learningPlanRepository = learningPlanRepository;
        this.sessionTaskRepository = sessionTaskRepository;
        this.learningSessionRepository = learningSessionRepository;
        this.jsonSerde = jsonSerde;
        this.taskSpecConverter = taskSpecConverter;
    }

    public PlanPreviewData preview(String goalId, String diagnosisId) {
        entityLookupGuard.requireGoal(goalId);
        sessionStateGuard.requireDiagnosisCompletedForPreview(diagnosisId);
        String sessionIdStr = store.getDiagnosisToSession().get(diagnosisId);
        if (sessionIdStr == null) {
            throw new BusinessException(BusinessErrorCode.RESOURCE_NOT_FOUND, "session not found for diagnosis: " + diagnosisId);
        }
        PlanningContext ctx = planningContextAssembler.assemble(goalId, diagnosisId);
        String strategy = planStrategySelector.select(ctx);
        String topicLabel = ctx.getGoal() != null && ctx.getGoal().getTopics() != null && !ctx.getGoal().getTopics().isEmpty()
                ? String.join("、", ctx.getGoal().getTopics()) : "当前主题";
        PlanTemplateFactory.StagesAndTasks stagesAndTasks = planTemplateFactory.build(strategy, "plan_pending", topicLabel, ctx);
        RecommendedEntry entry = recommendedEntryBuilder.build(strategy, ctx);
        RecommendedStrategy recommendedStrategy = toRecommendedStrategy(strategy);
        List<String> successCriteria = stagesAndTasks.tasks.stream()
                .flatMap(t -> t.getCompletionCriteria() != null ? t.getCompletionCriteria().stream() : java.util.stream.Stream.empty())
                .distinct()
                .limit(5)
                .collect(Collectors.toList());
        if (successCriteria.isEmpty()) successCriteria = List.of("完成各阶段任务目标");
        List<String> keyEvidence = planTemplateFactory.keyEvidence(ctx);
        List<String> risks = planTemplateFactory.risks(ctx, strategy);

        var preview = LearningPlanPreview.builder()
                .planId(null)
                .goalId(goalId)
                .recommendedEntry(entry)
                .recommendedStrategy(recommendedStrategy)
                .stages(stagesAndTasks.stages)
                .tasks(stagesAndTasks.tasks)
                .successCriteria(successCriteria)
                .keyEvidence(keyEvidence)
                .risks(risks)
                .previewOnly(true)
                .build();
        String planId = persistPreviewToDb(goalId, diagnosisId, sessionIdStr, strategy, preview);
        if (planId == null) {
            throw new BusinessException(BusinessErrorCode.INTERNAL_ERROR, "failed to persist plan preview");
        }
        preview.setPlanId(planId);
        store.getPlanPreviews().put(planId, preview);
        store.getPlanStatuses().put(planId, PlanStatus.PREVIEW_READY);
        String goalSummary = ctx.getGoal() != null && ctx.getGoal().getNormalizedGoalText() != null ? ctx.getGoal().getNormalizedGoalText() : "学习目标";
        return PlanPreviewData.builder()
                .planId(planId)
                .status(PlanStatus.PREVIEW_READY.name())
                .previewOnly(true)
                .committed(false)
                .goal(goalSummary)
                .recommendedEntry(entry)
                .recommendedStrategy(recommendedStrategy)
                .stages(stagesAndTasks.stages)
                .tasks(stagesAndTasks.tasks)
                .successCriteria(successCriteria)
                .keyEvidence(keyEvidence)
                .risks(risks)
                .build();
    }

    private static RecommendedStrategy toRecommendedStrategy(String strategy) {
        RecommendedStrategyCode code;
        String label;
        switch (strategy) {
            case PlanStrategySelector.FRAMEWORK_BUILD:
                code = RecommendedStrategyCode.FRAMEWORK_BUILD;
                label = "框架搭建";
                break;
            case PlanStrategySelector.FOUNDATION_PATCH:
                code = RecommendedStrategyCode.FOUNDATION_PATCH;
                label = "基础修补";
                break;
            case PlanStrategySelector.SPRINT_CORRECTION:
                code = RecommendedStrategyCode.SPRINT_CORRECTION;
                label = "冲刺纠偏";
                break;
            case PlanStrategySelector.DRILL_STRENGTHEN:
                code = RecommendedStrategyCode.DRILL_STRENGTHEN;
                label = "刷题强化";
                break;
            case PlanStrategySelector.LOCAL_REPAIR:
                code = RecommendedStrategyCode.LOCAL_REPAIR;
                label = "局部修补";
                break;
            case PlanStrategySelector.CONCEPT_CLARIFICATION:
            default:
                code = RecommendedStrategyCode.CONCEPT_CLARIFICATION;
                label = "概念澄清";
                break;
        }
        return RecommendedStrategy.builder()
                .code(code)
                .label(label)
                .reason("根据目标与诊断选择")
                .build();
    }

    public CommitPlanData commit(String planId) {
        entityLookupGuard.requirePlan(planId);
        PlanStatus currentStatus = store.getPlanStatuses().get(planId);
        if (currentStatus == PlanStatus.COMMITTED) {
            throw new BusinessException(BusinessErrorCode.PLAN_ALREADY_COMMITTED, "plan already committed");
        }
        if (currentStatus != PlanStatus.PREVIEW_READY) {
            throw new BusinessException(BusinessErrorCode.PLAN_NOT_COMMITTED, "plan not in preview state");
        }
        store.getPlanStatuses().put(planId, PlanStatus.COMMITTED);
        Long planDbId = extractNumericId(planId);
        boolean committed = learningPlanRepository.compareAndCommit(planDbId, PlanStatus.PREVIEW_READY);
        if (!committed) {
            throw new BusinessException(BusinessErrorCode.PLAN_NOT_COMMITTED, "plan not in preview state");
        }
        LearningPlanEntity planEntity = learningPlanRepository.findById(planDbId);
        if (planEntity == null) {
            throw new BusinessException(BusinessErrorCode.RESOURCE_NOT_FOUND, "plan not found: " + planId);
        }
        Long sessionDbId = planEntity.getSessionId();
        String sessionId = "learn_session_" + sessionDbId;
        LearningPlanPreview preview = store.getPlanPreviews().get(planId);
        List<String> taskSequence = preview.getTasks() != null
                ? preview.getTasks().stream().map(TaskBlueprint::getTaskId).collect(Collectors.toList())
                : List.of(FixedSampleData.TASK_001, FixedSampleData.TASK_002, FixedSampleData.TASK_003);
        if (taskSequence.isEmpty()) {
            taskSequence = List.of(FixedSampleData.TASK_001, FixedSampleData.TASK_002, FixedSampleData.TASK_003);
        }
        var state = new InMemoryStore.LearningSessionState();
        state.setSessionId(sessionId);
        state.setPlanId(planId);
        state.setTaskSequence(taskSequence);
        state.setCurrentTaskIndex(0);
        state.setStatus("IN_PROGRESS");
        store.getSessions().put(sessionId, state);
        materializeTasksToDb(sessionDbId, planDbId, taskSequence);
        materializeExecutableTaskSpecs(sessionId, planEntity, preview, taskSequence);
        String currentTaskId = taskSequence.isEmpty() ? null : taskSequence.get(0);
        return CommitPlanData.builder()
                .sessionId(sessionId)
                .planId(planId)
                .taskSequence(state.getTaskSequence())
                .currentTaskId(currentTaskId)
                .status("IN_PROGRESS")
                .build();
    }

    private String persistPreviewToDb(String goalId,
                                     String diagnosisId,
                                     String sessionIdStr,
                                     String strategy,
                                     LearningPlanPreview preview) {
        Long goalDbId = extractNumericId(goalId);
        Long sessionDbId = extractNumericId(sessionIdStr);
        Long diagnosisDbId = extractNumericId(diagnosisId);
        if (sessionDbId == null || goalDbId == null || diagnosisDbId == null) {
            return null;
        }
        LearningPlanEntity entity = new LearningPlanEntity();
        entity.setSessionId(sessionDbId);
        entity.setGoalId(goalDbId);
        entity.setDiagnosisSessionId(diagnosisDbId);
        entity.setStatus(PlanStatus.PREVIEW_READY.name());
        entity.setStrategyCode(strategy);
        entity.setRecommendedEntryJson(jsonSerde.toJson(preview.getRecommendedEntry()));
        entity.setPlanSnapshotJson(jsonSerde.toJson(preview));
        entity.setSuccessCriteriaJson(jsonSerde.toJson(preview.getSuccessCriteria()));
        entity.setRisksJson(jsonSerde.toJson(preview.getRisks()));
        entity.setKeyEvidenceJson(jsonSerde.toJson(preview.getKeyEvidence()));
        LearningPlanEntity saved = learningPlanRepository.savePreview(entity);
        return saved != null && saved.getId() != null ? "plan_" + saved.getId() : null;
    }

    private void materializeTasksToDb(Long sessionDbId, Long planDbId, java.util.List<String> taskSequence) {
        String planId = "plan_" + planDbId;
        LearningPlanPreview preview = store.getPlanPreviews().get(planId);
        if (sessionDbId == null || planDbId == null || preview == null || preview.getTasks() == null) {
            return;
        }
        java.util.Map<String, TaskBlueprint> blueprintById = preview.getTasks().stream()
                .collect(java.util.stream.Collectors.toMap(TaskBlueprint::getTaskId, t -> t));
        java.util.List<SessionTaskEntity> entities = new java.util.ArrayList<>();
        for (int i = 0; i < taskSequence.size(); i++) {
            String taskId = taskSequence.get(i);
            TaskBlueprint blueprint = blueprintById.get(taskId);
            if (blueprint == null) {
                continue;
            }
            SessionTaskEntity entity = new SessionTaskEntity();
            entity.setSessionId(sessionDbId);
            entity.setPlanId(planDbId);
            entity.setStageCode("STAGE_" + (i + 1));
            entity.setTaskCode(taskId);
            entity.setTaskType(blueprint.getTaskType() != null ? blueprint.getTaskType().name() : "UNKNOWN");
            entity.setOrderIndex(i);
            entity.setTitle(blueprint.getTitle());
            entity.setObjective(blueprint.getGoal());
            entity.setTaskSnapshotJson(jsonSerde.toJson(blueprint));
            entity.setCompletionCriteriaJson(jsonSerde.toJson(blueprint.getCompletionCriteria()));
            entity.setEstimatedMinutes(blueprint.getEstimatedMinutes());
            entity.setStatus(i == 0 ? "CURRENT" : "PENDING");
            entities.add(entity);
        }
        sessionTaskRepository.saveAll(entities);
        learningSessionRepository.updatePlanId(sessionDbId, planDbId);
        learningSessionRepository.markPlanCommitted(sessionDbId, taskSequence.size());
    }

    private void materializeExecutableTaskSpecs(String sessionId, LearningPlanEntity planEntity,
                                                LearningPlanPreview preview, List<String> taskSequence) {
        if (preview == null || preview.getTasks() == null) {
            return;
        }
        String goalId = "goal_" + planEntity.getGoalId();
        String diagnosisId = store.getDiagnosisToSession().entrySet().stream()
                .filter(e -> sessionId.equals(e.getValue()))
                .map(java.util.Map.Entry::getKey)
                .findFirst()
                .orElse(null);
        if (diagnosisId == null) {
            diagnosisId = "diagnosis_" + planEntity.getDiagnosisSessionId();
        }
        PlanningContext ctx = planningContextAssembler.assemble(goalId, diagnosisId);
        var learnerStrategyProfile = ctx.getLearnerStrategyProfile();
        TimeBudgetConstraint timeBudgetConstraint = ctx.getTimeBudgetConstraint();
        var blueprintById = preview.getTasks().stream()
                .collect(Collectors.toMap(TaskBlueprint::getTaskId, t -> t));
        for (String taskId : taskSequence) {
            TaskBlueprint bp = blueprintById.get(taskId);
            if (bp == null) {
                continue;
            }
            ExecutableTaskSpec spec = taskSpecConverter.convert(bp, learnerStrategyProfile, timeBudgetConstraint);
            if (spec != null) {
                store.getExecutableTaskSpecs().put(InMemoryStore.taskRuntimeKey(sessionId, taskId), spec);
            }
        }
    }

    private Long extractNumericId(String id) {
        if (id == null) {
            return null;
        }
        String digits = id.replaceAll("\\D+", "");
        if (digits.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(digits);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
