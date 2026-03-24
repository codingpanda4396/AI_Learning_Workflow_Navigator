package navigator.application.rule.guidance.rules;

import navigator.application.rule.guidance.GuidancePhaseRule;
import navigator.domain.enums.LearningGuidancePhase;
import navigator.domain.model.LearningEvidence;
import org.springframework.stereotype.Component;

@Component
public class MetaReflectRule implements GuidancePhaseRule {

    @Override
    public String getId() {
        return "META_REFLECT_RULE";
    }

    @Override
    public int getPriority() {
        return 60;
    }

    @Override
    public boolean match(LearningEvidence evidence) {
        return evidence != null
                && evidence.isAttemptedExplain()
                && evidence.isContainsKeyConcept()
                && !evidence.isConfusedSignal()
                && !evidence.isAskingForAnswer()
                && evidence.getInteractionDepth() >= 2;
    }

    @Override
    public LearningGuidancePhase apply(LearningEvidence evidence) {
        return LearningGuidancePhase.META_REFLECT;
    }

    @Override
    public String reason(LearningEvidence evidence) {
        return "User has articulated key concepts and can move to reflection";
    }
}
