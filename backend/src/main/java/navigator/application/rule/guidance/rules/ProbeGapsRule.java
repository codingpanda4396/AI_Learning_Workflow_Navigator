package navigator.application.rule.guidance.rules;

import navigator.application.rule.guidance.GuidancePhaseRule;
import navigator.domain.enums.LearningGuidancePhase;
import navigator.domain.model.LearningEvidence;
import org.springframework.stereotype.Component;

@Component
public class ProbeGapsRule implements GuidancePhaseRule {

    @Override
    public String getId() {
        return "PROBE_GAPS_RULE";
    }

    @Override
    public int getPriority() {
        return 70;
    }

    @Override
    public boolean match(LearningEvidence evidence) {
        return evidence != null
                && (evidence.isConfusedSignal()
                || evidence.isHasMisconception()
                || evidence.isAttemptedExplain() && !evidence.isContainsKeyConcept());
    }

    @Override
    public LearningGuidancePhase apply(LearningEvidence evidence) {
        return LearningGuidancePhase.PROBE_GAPS;
    }

    @Override
    public String reason(LearningEvidence evidence) {
        return "Current evidence shows confusion, misconception, or concept gaps";
    }
}
