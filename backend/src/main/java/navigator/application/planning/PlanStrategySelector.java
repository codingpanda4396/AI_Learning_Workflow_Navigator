package navigator.application.planning;

import navigator.application.rule.engine.RuleEngine;
import navigator.application.rule.engine.RuleResult;
import navigator.application.rule.planning.PlanningRule;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PlanStrategySelector {

    public static final String FOUNDATION_PATCH = "FOUNDATION_PATCH";
    public static final String FRAMEWORK_BUILD = "FRAMEWORK_BUILD";
    public static final String DRILL_STRENGTHEN = "DRILL_STRENGTHEN";
    public static final String SPRINT_CORRECTION = "SPRINT_CORRECTION";
    public static final String LOCAL_REPAIR = "LOCAL_REPAIR";
    public static final String CONCEPT_CLARIFICATION = "CONCEPT_CLARIFICATION";

    private static final String DEFAULT_RULE_ID = "DEFAULT_CONCEPT_CLARIFICATION_RULE";
    private static final String DEFAULT_REASON = "Planning context is incomplete, fallback to concept clarification";

    private final RuleEngine<PlanningContext, String> engine;

    public PlanStrategySelector(List<PlanningRule> rules) {
        this.engine = new RuleEngine<>(rules);
    }

    public String select(PlanningContext ctx) {
        return selectResult(ctx).getResult();
    }

    public RuleResult<String> selectResult(PlanningContext ctx) {
        if (ctx == null || ctx.getGoal() == null) {
            return RuleResult.<String>builder()
                    .result(CONCEPT_CLARIFICATION)
                    .ruleId(DEFAULT_RULE_ID)
                    .reason(DEFAULT_REASON)
                    .priority(Integer.MIN_VALUE)
                    .build();
        }
        return engine.execute(ctx);
    }
}
