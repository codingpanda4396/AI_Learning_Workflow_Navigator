package navigator.application.rule.planning.rules;

import navigator.application.planning.PlanStrategySelector;
import navigator.application.planning.PlanningContext;
import navigator.application.rule.planning.PlanningRule;
import navigator.application.rule.planning.PlanningRuleSupport;
import navigator.domain.enums.GoalType;
import org.springframework.stereotype.Component;

@Component
public class LocalRepairRule implements PlanningRule {

    @Override
    public String getId() {
        return "LOCAL_REPAIR_RULE";
    }

    @Override
    public int getPriority() {
        return 80;
    }

    @Override
    public boolean match(PlanningContext context) {
        return PlanningRuleSupport.hasGoal(context)
                && PlanningRuleSupport.isGoalType(context, GoalType.FIX_SPECIFIC_BLOCKER)
                && PlanningRuleSupport.isScope(context, "SINGLE_TOPIC");
    }

    @Override
    public String apply(PlanningContext context) {
        return PlanStrategySelector.LOCAL_REPAIR;
    }

    @Override
    public String reason(PlanningContext context) {
        return "Single-topic blocker should use local repair";
    }
}
