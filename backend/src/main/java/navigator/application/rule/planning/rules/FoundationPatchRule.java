package navigator.application.rule.planning.rules;

import navigator.application.planning.PlanStrategySelector;
import navigator.application.planning.PlanningContext;
import navigator.application.rule.planning.PlanningRule;
import navigator.application.rule.planning.PlanningRuleSupport;
import org.springframework.stereotype.Component;

@Component
public class FoundationPatchRule implements PlanningRule {

    @Override
    public String getId() {
        return "FOUNDATION_PATCH_RULE";
    }

    @Override
    public int getPriority() {
        return 110;
    }

    @Override
    public boolean match(PlanningContext context) {
        return PlanningRuleSupport.hasGoal(context)
                && (PlanningRuleSupport.isBeginner(context)
                || PlanningRuleSupport.hasRisk(context, "PREREQUISITE_GAP"));
    }

    @Override
    public String apply(PlanningContext context) {
        return PlanStrategySelector.FOUNDATION_PATCH;
    }

    @Override
    public String reason(PlanningContext context) {
        return "Beginner foundation or prerequisite gap requires a foundation patch";
    }
}
