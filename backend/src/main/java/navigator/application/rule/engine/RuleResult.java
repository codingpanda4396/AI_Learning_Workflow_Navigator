package navigator.application.rule.engine;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RuleResult<T> {
    private T result;
    private String ruleId;
    private String reason;
    private int priority;
}
