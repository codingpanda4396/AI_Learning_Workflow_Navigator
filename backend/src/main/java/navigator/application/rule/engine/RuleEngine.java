package navigator.application.rule.engine;

import java.util.Comparator;
import java.util.List;

public class RuleEngine<TContext, TResult> {

    private final List<Rule<TContext, TResult>> rules;

    public RuleEngine(List<? extends Rule<TContext, TResult>> rules) {
        this.rules = List.copyOf(rules);
    }

    public RuleResult<TResult> execute(TContext context) {
        return rules.stream()
                .filter(rule -> rule.match(context))
                .sorted(Comparator.comparingInt((Rule<TContext, TResult> rule) -> rule.getPriority()).reversed())
                .findFirst()
                .map(rule -> RuleResult.<TResult>builder()
                        .result(rule.apply(context))
                        .ruleId(rule.getId())
                        .reason(rule.reason(context))
                        .priority(rule.getPriority())
                        .build())
                .orElseThrow(() -> new IllegalStateException("No matching rule"));
    }
}
