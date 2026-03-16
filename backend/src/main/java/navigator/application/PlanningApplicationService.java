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

    public PlanningApplicationService(InMemoryStore store, EntityLookupGuard entityLookupGuard, SessionStateGuard sessionStateGuard,
                                      PlanningContextAssembler planningContextAssembler, PlanStrategySelector planStrategySelector,
                                      RecommendedEntryBuilder recommendedEntryBuilder, PlanTemplateFactory planTemplateFactory) {
        this.store = store;
        this.entityLookupGuard = entityLookupGuard;
        this.sessionStateGuard = sessionStateGuard;
        this.planningContextAssembler = planningContextAssembler;
        this.planStrategySelector = planStrategySelector;
        this.recommendedEntryBuilder = recommendedEntryBuilder;
        this.planTemplateFactory = planTemplateFactory;
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
            case PlanStrategySelector.SYSTEMATIC_PROGRESSIVE:
                code = RecommendedStrategyCode.PROGRESSIVE_SYSTEMATIC;
                label = "系统渐进";
                break;
            case PlanStrategySelector.FOUNDATION_REBUILD:
                code = RecommendedStrategyCode.REBUILD_FOUNDATION;
                label = "前置重建";
                break;
            case PlanStrategySelector.COMPRESSED_REVIEW:
                code = RecommendedStrategyCode.COMPRESSED_REVIEW;
                label = "压缩复习";
                break;
            case PlanStrategySelector.PRACTICE_DRIVEN:
                code = RecommendedStrategyCode.PRACTICE_WITH_SCAFFOLD;
                label = "练习驱动";
                break;
            case PlanStrategySelector.LOCAL_REPAIR:
                code = RecommendedStrategyCode.PATCH_PREREQUISITE;
                label = "局部修补";
                break;
            case PlanStrategySelector.CONCEPT_CLARIFICATION:
            default:
                code = RecommendedStrategyCode.CLARIFY_CORE_CONCEPT;
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
        String currentTaskId = taskSequence.isEmpty() ? null : taskSequence.get(0);
        return CommitPlanData.builder()
                .sessionId(FixedSampleData.SESSION_ID)
                .planId(planId)
                .taskSequence(state.getTaskSequence())
                .currentTaskId(currentTaskId)
                .status("IN_PROGRESS")
                .build();
    }
}
