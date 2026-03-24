package navigator.application.rule.guidance.rules;

import navigator.application.rule.guidance.GuidancePhaseRule;
import navigator.domain.enums.LearningGuidancePhase;
import navigator.domain.model.LearningEvidence;
import org.springframework.stereotype.Component;

@Component
public class TransitionHintRule implements GuidancePhaseRule {

    @Override
    public String getId() {
        return "TRANSITION_HINT_RULE";
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public boolean match(LearningEvidence evidence) {
        return true;
    }

    @Override
    public LearningGuidancePhase apply(LearningEvidence evidence) {
        return LearningGuidancePhase.TRANSITION_HINT;
    }

    @Override
    public String reason(LearningEvidence evidence) {
        return "Default transition hint when no stronger guidance rule matches";
    }
}
