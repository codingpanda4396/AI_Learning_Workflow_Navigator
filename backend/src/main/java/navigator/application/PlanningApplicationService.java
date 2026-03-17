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
import navigator.domain.model.LearningPlanPreview;
import navigator.domain.model.RecommendedEntry;
import navigator.domain.model.RecommendedStrategy;
import navigator.domain.model.TaskBlueprint;
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
                                      JsonSerde jsonSerde) {
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
    }

    public PlanPreviewData preview(String goalId, String diagnosisId) {
        entityLookupGuard.requireGoal(goalId);
        sessionStateGuard.requireDiagnosisCompletedForPreview(diagnosisId);
        PlanningContext ctx = planningContextAssembler.assemble(goalId, diagnosisId);
        String strategy = planStrategySelector.select(ctx);
        String topicLabel = ctx.getGoal() != null && ctx.getGoal().getTopics() != null && !ctx.getGoal().getTopics().isEmpty()
                ? String.join("、", ctx.getGoal().getTopics()) : "当前主题";
        PlanTemplateFactory.StagesAndTasks stagesAndTasks = planTemplateFactory.build(strategy, FixedSampleData.PLAN_ID, topicLabel);
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
                .planId(FixedSampleData.PLAN_ID)
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
        store.getPlanPreviews().put(FixedSampleData.PLAN_ID, preview);
        store.getPlanStatuses().put(FixedSampleData.PLAN_ID, PlanStatus.PREVIEW_READY);
        // 持久化 learning_plan 预览快照
        persistPreviewToDb(goalId, diagnosisId, strategy, preview);
        String goalSummary = ctx.getGoal() != null && ctx.getGoal().getNormalizedGoalText() != null ? ctx.getGoal().getNormalizedGoalText() : "学习目标";
        return PlanPreviewData.builder()
                .planId(FixedSampleData.PLAN_ID)
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
        // CAS 风格推进 DB 中的 plan 状态
        Long planDbId = extractNumericId(planId);
        boolean committed = learningPlanRepository.compareAndCommit(planDbId, PlanStatus.PREVIEW_READY);
        if (!committed) {
            throw new BusinessException(BusinessErrorCode.PLAN_NOT_COMMITTED, "plan not in preview state");
        }
        LearningPlanPreview preview = store.getPlanPreviews().get(planId);
        List<String> taskSequence = preview.getTasks() != null
                ? preview.getTasks().stream().map(TaskBlueprint::getTaskId).collect(Collectors.toList())
                : List.of(FixedSampleData.TASK_001, FixedSampleData.TASK_002, FixedSampleData.TASK_003);
        if (taskSequence.isEmpty()) {
            taskSequence = List.of(FixedSampleData.TASK_001, FixedSampleData.TASK_002, FixedSampleData.TASK_003);
        }
        var state = new InMemoryStore.LearningSessionState();
        state.setSessionId(FixedSampleData.SESSION_ID);
        state.setPlanId(planId);
        state.setTaskSequence(taskSequence);
        state.setCurrentTaskIndex(0);
        state.setStatus("IN_PROGRESS");
        store.getSessions().put(FixedSampleData.SESSION_ID, state);
        // 物化任务到 session_task 表，并更新 learning_session
        materializeTasksToDb(planDbId, taskSequence);
        String currentTaskId = taskSequence.isEmpty() ? null : taskSequence.get(0);
        return CommitPlanData.builder()
                .sessionId(FixedSampleData.SESSION_ID)
                .planId(planId)
                .taskSequence(state.getTaskSequence())
                .currentTaskId(currentTaskId)
                .status("IN_PROGRESS")
                .build();
    }

    private void persistPreviewToDb(String goalId,
                                    String diagnosisId,
                                    String strategy,
                                    LearningPlanPreview preview) {
        Long planDbId = extractNumericId(preview.getPlanId());
        Long goalDbId = extractNumericId(goalId);
        Long sessionDbId = extractNumericId(FixedSampleData.SESSION_ID);
        Long diagnosisDbId = extractNumericId(diagnosisId);
        if (sessionDbId == null || goalDbId == null || diagnosisDbId == null) {
            return;
        }
        LearningPlanEntity entity = new LearningPlanEntity();
        entity.setId(planDbId);
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
        learningPlanRepository.savePreview(entity);
    }

    private void materializeTasksToDb(Long planDbId, java.util.List<String> taskSequence) {
        Long sessionDbId = extractNumericId(FixedSampleData.SESSION_ID);
        LearningPlanPreview preview = store.getPlanPreviews().get(FixedSampleData.PLAN_ID);
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
        // learning_session 中写入 planId、current_task_id、total_task_count 等，由后续阶段细化
        learningSessionRepository.markDiagnosisCompleted(sessionDbId);
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
