package navigator.application.task.guidance;

import navigator.application.learning.LearningEvidenceBuilder;
import navigator.application.rule.engine.RuleResult;
import navigator.application.task.TaskExecutionRuntime;
import navigator.domain.enums.LearningActionType;
import navigator.domain.enums.LearningGuidancePhase;
import navigator.domain.model.LearningEvidence;
import org.springframework.stereotype.Component;

@Component
public class GuidancePhaseTransitionEvaluator {

    private final LearningEvidenceBuilder evidenceBuilder;
    private final GuidanceRuleEngine guidanceRuleEngine;

    public GuidancePhaseTransitionEvaluator(LearningEvidenceBuilder evidenceBuilder,
                                            GuidanceRuleEngine guidanceRuleEngine) {
        this.evidenceBuilder = evidenceBuilder;
        this.guidanceRuleEngine = guidanceRuleEngine;
    }

    public RuleResult<LearningGuidancePhase> evaluate(TaskExecutionRuntime runtime,
                                                      LearningActionType action,
                                                      String userInput) {
        LearningEvidence evidence = evidenceBuilder.build(runtime, action, userInput);
        return guidanceRuleEngine.execute(evidence);
    }
}
