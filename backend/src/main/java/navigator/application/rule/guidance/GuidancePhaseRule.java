package navigator.application.rule.guidance;

import navigator.application.rule.engine.Rule;
import navigator.domain.enums.LearningGuidancePhase;
import navigator.domain.model.LearningEvidence;

public interface GuidancePhaseRule extends Rule<LearningEvidence, LearningGuidancePhase> {
}
