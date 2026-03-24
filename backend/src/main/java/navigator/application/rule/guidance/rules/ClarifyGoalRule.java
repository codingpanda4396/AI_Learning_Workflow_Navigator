package navigator.application.rule.guidance.rules;

import navigator.application.rule.guidance.GuidancePhaseRule;
import navigator.domain.enums.LearningGuidancePhase;
import navigator.domain.model.LearningEvidence;
import org.springframework.stereotype.Component;

@Component
public class ClarifyGoalRule implements GuidancePhaseRule {

    @Override
    public String getId() {
        return "CLARIFY_GOAL_RULE";
    }

    @Override
    public int getPriority() {
        return 100;
    }

    @Override
    public boolean match(LearningEvidence evidence) {
        return evidence != null
                && evidence.getInteractionDepth() <= 1
                && !evidence.isAttemptedExplain()
                && !evidence.isConfusedSignal();
    }

    @Override
    public LearningGuidancePhase apply(LearningEvidence evidence) {
        return LearningGuidancePhase.CLARIFY_GOAL;
    }

    @Override
    public String reason(LearningEvidence evidence) {
        return "Need to align the task goal before deeper guidance";
    }
}
