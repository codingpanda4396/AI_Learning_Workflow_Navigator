package navigator.application.task.guidance;

import navigator.application.rule.engine.RuleEngine;
import navigator.application.rule.engine.RuleResult;
import navigator.application.rule.guidance.GuidancePhaseRule;
import navigator.domain.enums.LearningGuidancePhase;
import navigator.domain.model.LearningEvidence;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GuidanceRuleEngine {

    private final RuleEngine<LearningEvidence, LearningGuidancePhase> engine;

    public GuidanceRuleEngine(List<GuidancePhaseRule> rules) {
        this.engine = new RuleEngine<>(rules);
    }

    public RuleResult<LearningGuidancePhase> execute(LearningEvidence evidence) {
        return engine.execute(evidence);
    }
}
