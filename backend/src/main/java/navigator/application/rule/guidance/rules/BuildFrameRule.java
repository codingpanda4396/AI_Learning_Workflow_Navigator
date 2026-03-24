package navigator.application.rule.guidance.rules;

import navigator.application.rule.guidance.GuidancePhaseRule;
import navigator.domain.enums.LearningGuidancePhase;
import navigator.domain.model.LearningEvidence;
import org.springframework.stereotype.Component;

@Component
public class BuildFrameRule implements GuidancePhaseRule {

    @Override
    public String getId() {
        return "BUILD_FRAME_RULE";
    }

    @Override
    public int getPriority() {
        return 90;
    }

    @Override
    public boolean match(LearningEvidence evidence) {
        return evidence != null
                && evidence.getInteractionDepth() >= 2
                && !evidence.isAttemptedExplain()
                && !evidence.isContainsKeyConcept();
    }

    @Override
    public LearningGuidancePhase apply(LearningEvidence evidence) {
        return LearningGuidancePhase.BUILD_FRAME;
    }

    @Override
    public String reason(LearningEvidence evidence) {
        return "User has not articulated key concepts yet, so build a frame first";
    }
}
