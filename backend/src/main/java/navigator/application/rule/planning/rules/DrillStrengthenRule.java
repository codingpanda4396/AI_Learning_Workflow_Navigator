package navigator.application.rule.planning.rules;

import navigator.application.planning.PlanStrategySelector;
import navigator.application.planning.PlanningContext;
import navigator.application.rule.planning.PlanningRule;
import navigator.application.rule.planning.PlanningRuleSupport;
import navigator.domain.enums.GoalType;
import org.springframework.stereotype.Component;

@Component
public class DrillStrengthenRule implements PlanningRule {

    @Override
    public String getId() {
        return "DRILL_STRENGTHEN_RULE";
    }

    @Override
    public int getPriority() {
        return 90;
    }

    @Override
    public boolean match(PlanningContext context) {
        return PlanningRuleSupport.hasGoal(context)
                && (PlanningRuleSupport.hasPrimaryGap(context, "QUESTION_TYPE_RECOGNITION_GAP")
                || PlanningRuleSupport.hasPrimaryGap(context, "PROCEDURE_GAP")
                || PlanningRuleSupport.isGoalType(context, GoalType.PRACTICE_ENHANCEMENT));
    }

    @Override
    public String apply(PlanningContext context) {
        return PlanStrategySelector.DRILL_STRENGTHEN;
    }

    @Override
    public String reason(PlanningContext context) {
        return "Procedure or practice gap should use drill strengthen";
    }
}
