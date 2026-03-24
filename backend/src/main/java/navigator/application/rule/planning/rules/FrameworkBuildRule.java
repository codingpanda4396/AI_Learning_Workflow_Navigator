package navigator.application.rule.planning.rules;

import navigator.application.planning.PlanStrategySelector;
import navigator.application.planning.PlanningContext;
import navigator.application.rule.planning.PlanningRule;
import navigator.application.rule.planning.PlanningRuleSupport;
import navigator.domain.enums.GoalType;
import org.springframework.stereotype.Component;

@Component
public class FrameworkBuildRule implements PlanningRule {

    @Override
    public String getId() {
        return "FRAMEWORK_BUILD_RULE";
    }

    @Override
    public int getPriority() {
        return 120;
    }

    @Override
    public boolean match(PlanningContext context) {
        return PlanningRuleSupport.hasGoal(context)
                && (PlanningRuleSupport.isChapterOrCourse(context)
                || PlanningRuleSupport.isGoalType(context, GoalType.BUILD_SYSTEMATIC_UNDERSTANDING));
    }

    @Override
    public String apply(PlanningContext context) {
        return PlanStrategySelector.FRAMEWORK_BUILD;
    }

    @Override
    public String reason(PlanningContext context) {
        return "Large scope or systematic goal should build a framework first";
    }
}
