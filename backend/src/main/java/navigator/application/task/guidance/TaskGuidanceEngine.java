package navigator.application.task.guidance;

import navigator.domain.enums.LearningActionType;
import navigator.domain.enums.TaskExecutionState;
import navigator.domain.model.GuidanceDecision;
import navigator.domain.model.LearnerStrategyProfile;
import navigator.domain.model.TaskScaffold;
import navigator.domain.policy.tutor.TutorInteractionPolicy;
import navigator.application.task.TaskExecutionRuntime;
import org.springframework.stereotype.Component;

/**
 * 任务引导编排入口：状态合法时产出 {@link GuidanceDecision}。
 */
@Component
public class TaskGuidanceEngine {

    private final TutorInteractionPolicy tutorInteractionPolicy;
    private final GuidancePhaseTransitionEvaluator phaseTransitionEvaluator;

    public TaskGuidanceEngine(TutorInteractionPolicy tutorInteractionPolicy,
                              GuidancePhaseTransitionEvaluator phaseTransitionEvaluator) {
        this.tutorInteractionPolicy = tutorInteractionPolicy;
        this.phaseTransitionEvaluator = phaseTransitionEvaluator;
    }

    public GuidanceDecision decideForExploreTurn(TaskExecutionRuntime rt,
                                                   LearningActionType action,
                                                   TaskScaffold scaffold,
                                                   LearnerStrategyProfile strategyProfile) {
        TaskExecutionState st = rt.getState();
        if (st != TaskExecutionState.EXPLORE && st != TaskExecutionState.REMEDIAL) {
            return null;
        }
        var phase = rt.getGuidancePhase() != null ? rt.getGuidancePhase()
                : navigator.domain.enums.LearningGuidancePhase.CLARIFY_GOAL;
        GuidanceDecision base = tutorInteractionPolicy.decide(phase, action, strategyProfile);
        base.setPhase(phase);
        return base;
    }

    /** 在 exploreTurnCount 递增之后调用，使当轮策略与轮次一致 */
    public void syncGuidancePhaseFromExploreCount(TaskExecutionRuntime rt) {
        if (rt.getState() != TaskExecutionState.EXPLORE && rt.getState() != TaskExecutionState.REMEDIAL) {
            return;
        }
        int n = rt.getExploreTurnCount();
        var phase = phaseTransitionEvaluator.phaseForExploreCount(n);
        rt.setGuidancePhase(phase);
        TaskExecutionEvidenceAccumulator.ensureSnapshot(rt);
        if (phase != null) {
            rt.getEvidenceSnapshot().getCompletedGuidancePhases().add(phase);
        }
    }
}
