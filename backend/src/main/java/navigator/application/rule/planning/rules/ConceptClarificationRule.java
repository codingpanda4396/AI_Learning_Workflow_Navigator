package navigator.application.rule.planning.rules;

import navigator.application.planning.PlanStrategySelector;
import navigator.application.planning.PlanningContext;
import navigator.application.rule.planning.PlanningRule;
import navigator.application.rule.planning.PlanningRuleSupport;
import org.springframework.stereotype.Component;

@Component
public class ConceptClarificationRule implements PlanningRule {

    @Override
    public String getId() {
        return "CONCEPT_CLARIFICATION_RULE";
    }

    @Override
    public int getPriority() {
        return 0;
    }

    @Override
    public boolean match(PlanningContext context) {
        return PlanningRuleSupport.hasGoal(context);
    }

    @Override
    public String apply(PlanningContext context) {
        return PlanStrategySelector.CONCEPT_CLARIFICATION;
    }

    @Override
    public String reason(PlanningContext context) {
        return "Fallback to concept clarification when no stronger rule matches";
    }
}
