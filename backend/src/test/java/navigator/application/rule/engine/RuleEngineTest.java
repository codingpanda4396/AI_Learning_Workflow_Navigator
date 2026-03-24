package navigator.application.rule.engine;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RuleEngineTest {

    @Test
    void shouldPickHighestPriorityMatchingRule() {
        Rule<String, String> lowPriorityRule = new SimpleRule("LOW", 10, true, "LOW_RESULT");
        Rule<String, String> highPriorityRule = new SimpleRule("HIGH", 100, true, "HIGH_RESULT");
        RuleEngine<String, String> engine = new RuleEngine<>(List.of(lowPriorityRule, highPriorityRule));

        RuleResult<String> result = engine.execute("ctx");

        assertThat(result.getResult()).isEqualTo("HIGH_RESULT");
        assertThat(result.getRuleId()).isEqualTo("HIGH");
        assertThat(result.getReason()).isEqualTo("reason:HIGH");
        assertThat(result.getPriority()).isEqualTo(100);
    }

    private record SimpleRule(String id, int priority, boolean matches, String result) implements Rule<String, String> {

        @Override
        public String getId() {
            return id;
        }

        @Override
        public int getPriority() {
            return priority;
        }

        @Override
        public boolean match(String context) {
            return matches;
        }

        @Override
        public String apply(String context) {
            return result;
        }

        @Override
        public String reason(String context) {
            return "reason:" + id;
        }
    }
}
