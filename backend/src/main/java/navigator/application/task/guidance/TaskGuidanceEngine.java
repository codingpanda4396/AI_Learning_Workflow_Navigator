package navigator.application.task.guidance;

import navigator.application.rule.engine.RuleResult;
import navigator.application.task.TaskExecutionRuntime;
import navigator.domain.enums.LearningActionType;
import navigator.domain.enums.LearningGuidancePhase;
import navigator.domain.enums.TaskExecutionState;
import navigator.domain.model.GuidanceDecision;
import navigator.domain.model.LearnerStrategyProfile;
import navigator.domain.model.TaskScaffold;
import navigator.domain.policy.tutor.TutorInteractionPolicy;
import org.springframework.stereotype.Component;

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
        LearningGuidancePhase phase = rt.getGuidancePhase() != null
                ? rt.getGuidancePhase()
                : LearningGuidancePhase.CLARIFY_GOAL;
        GuidanceDecision base = tutorInteractionPolicy.decide(phase, action, strategyProfile);
        base.setPhase(phase);
        return base;
    }

    public RuleResult<LearningGuidancePhase> syncGuidancePhase(TaskExecutionRuntime rt,
                                                               LearningActionType action,
                                                               String userInput) {
        if (rt.getState() != TaskExecutionState.EXPLORE && rt.getState() != TaskExecutionState.REMEDIAL) {
            LearningGuidancePhase phase = rt.getGuidancePhase() != null
                    ? rt.getGuidancePhase()
                    : LearningGuidancePhase.CLARIFY_GOAL;
            return RuleResult.<LearningGuidancePhase>builder()
                    .result(phase)
                    .ruleId("GUIDANCE_PHASE_SKIPPED")
                    .reason("Guidance phase stays unchanged outside EXPLORE or REMEDIAL")
                    .priority(Integer.MIN_VALUE)
                    .build();
        }
        RuleResult<LearningGuidancePhase> result = phaseTransitionEvaluator.evaluate(rt, action, userInput);
        LearningGuidancePhase phase = result.getResult();
        rt.setGuidancePhase(phase);
        TaskExecutionEvidenceAccumulator.ensureSnapshot(rt);
        if (phase != null) {
            rt.getEvidenceSnapshot().getCompletedGuidancePhases().add(phase);
        }
        return result;
    }
}
