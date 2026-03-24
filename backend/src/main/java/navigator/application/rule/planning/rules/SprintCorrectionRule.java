package navigator.application.rule.planning.rules;

import navigator.application.planning.PlanStrategySelector;
import navigator.application.planning.PlanningContext;
import navigator.application.rule.planning.PlanningRule;
import navigator.application.rule.planning.PlanningRuleSupport;
import navigator.domain.enums.GoalType;
import org.springframework.stereotype.Component;

@Component
public class SprintCorrectionRule implements PlanningRule {

    @Override
    public String getId() {
        return "SPRINT_CORRECTION_RULE";
    }

    @Override
    public int getPriority() {
        return 100;
    }

    @Override
    public boolean match(PlanningContext context) {
        return PlanningRuleSupport.hasGoal(context)
                && PlanningRuleSupport.isGoalType(context, GoalType.REVIEW_FOR_EXAM)
                && PlanningRuleSupport.hasHighUrgency(context)
                && !PlanningRuleSupport.isBeginner(context);
    }

    @Override
    public String apply(PlanningContext context) {
        return PlanStrategySelector.SPRINT_CORRECTION;
    }

    @Override
    public String reason(PlanningContext context) {
        return "High urgency exam review should use sprint correction";
    }
}
