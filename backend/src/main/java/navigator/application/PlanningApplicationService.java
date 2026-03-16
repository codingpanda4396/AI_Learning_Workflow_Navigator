package navigator.application;

import navigator.api.dto.CommitPlanData;
import navigator.api.dto.PlanPreviewData;
import navigator.domain.enums.PlanStatus;
import navigator.infrastructure.memory.InMemoryStore;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class PlanningApplicationService {

    private final InMemoryStore store;

    public PlanningApplicationService(InMemoryStore store) {
        this.store = store;
    }

    public PlanPreviewData preview(String goalId, String diagnosisId) {
        var preview = navigator.domain.model.LearningPlanPreview.builder()
                .planId(FixedSampleData.PLAN_ID)
                .goalId(goalId)
                .recommendedEntry(FixedSampleData.recommendedEntry())
                .recommendedStrategy(FixedSampleData.recommendedStrategy())
                .stages(FixedSampleData.planStages())
                .tasks(FixedSampleData.taskBlueprints())
                .successCriteria(List.of(
                        "能解释链表的基本组成",
                        "能区分链表和数组的一个核心差异",
                        "能完成最小自我解释"
                ))
                .keyEvidence(List.of(
                        "诊断显示主要缺口在概念理解",
                        "当前时间预算适合 3 个小任务"
                ))
                .risks(List.of("如果跳过概念澄清，后续容易停留在术语记忆层"))
                .previewOnly(true)
                .build();
        store.getPlanPreviews().put(FixedSampleData.PLAN_ID, preview);
        return PlanPreviewData.builder()
                .planId(FixedSampleData.PLAN_ID)
                .status(PlanStatus.PREVIEW_READY.name())
                .previewOnly(true)
                .committed(false)
                .goal("理解链表")
                .recommendedEntry(FixedSampleData.recommendedEntry())
                .recommendedStrategy(FixedSampleData.recommendedStrategy())
                .stages(FixedSampleData.planStages())
                .tasks(FixedSampleData.taskBlueprints())
                .successCriteria(preview.getSuccessCriteria())
                .keyEvidence(preview.getKeyEvidence())
                .risks(preview.getRisks())
                .build();
    }

    public CommitPlanData commit(String planId) {
        var state = new InMemoryStore.LearningSessionState();
        state.setSessionId(FixedSampleData.SESSION_ID);
        state.setPlanId(planId);
        state.setTaskSequence(List.of(FixedSampleData.TASK_001, FixedSampleData.TASK_002, FixedSampleData.TASK_003));
        state.setCurrentTaskIndex(0);
        state.setStatus("IN_PROGRESS");
        store.getSessions().put(FixedSampleData.SESSION_ID, state);
        return CommitPlanData.builder()
                .sessionId(FixedSampleData.SESSION_ID)
                .planId(planId)
                .taskSequence(state.getTaskSequence())
                .currentTaskId(FixedSampleData.TASK_001)
                .status("IN_PROGRESS")
                .build();
    }
}
