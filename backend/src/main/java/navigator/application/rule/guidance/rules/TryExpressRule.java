package navigator.application.rule.guidance.rules;

import navigator.application.rule.guidance.GuidancePhaseRule;
import navigator.domain.enums.LearningGuidancePhase;
import navigator.domain.model.LearningEvidence;
import org.springframework.stereotype.Component;

@Component
public class TryExpressRule implements GuidancePhaseRule {

    @Override
    public String getId() {
        return "TRY_EXPRESS_RULE";
    }

    @Override
    public int getPriority() {
        return 80;
    }

    @Override
    public boolean match(LearningEvidence evidence) {
        return evidence != null
                && !evidence.isAttemptedExplain()
                && !evidence.isConfusedSignal()
                && evidence.getInteractionDepth() >= 2;
    }

    @Override
    public LearningGuidancePhase apply(LearningEvidence evidence) {
        return LearningGuidancePhase.TRY_EXPRESS;
    }

    @Override
    public String reason(LearningEvidence evidence) {
        return "User has not tried to express understanding yet";
    }
}
