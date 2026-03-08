package com.pandanav.learning.application.service.llm;

import com.pandanav.learning.domain.llm.model.EvaluationContext;
import com.pandanav.learning.domain.llm.model.EvaluationResult;
import com.pandanav.learning.domain.enums.Stage;
import com.pandanav.learning.infrastructure.config.LlmProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompositeAnswerEvaluatorTest {

    @Mock
    private LlmAnswerEvaluator llmAnswerEvaluator;
    @Mock
    private RuleBasedAnswerEvaluator ruleBasedAnswerEvaluator;

    @Test
    void shouldFallbackToRuleWhenLlmFails() {
        LlmProperties properties = new LlmProperties();
        properties.setEnabled(true);
        properties.setBaseUrl("http://localhost");
        properties.setApiKey("k");
        properties.setModel("m");
        properties.setFallbackToRule(true);

        EvaluationContext context = new EvaluationContext(1L, 1L, 1L, "obj", "q", "a", null, Stage.TRAINING);
        EvaluationResult fallback = new EvaluationResult(
            60, new BigDecimal("0.600"), "rule", List.of("MISSING_STEPS"), List.of(), List.of(), "INSERT_TRAINING_VARIANTS",
            null, null, null, "rule-v1", null
        );
        when(llmAnswerEvaluator.evaluate(any())).thenThrow(new RuntimeException("boom"));
        when(ruleBasedAnswerEvaluator.evaluate(any())).thenReturn(fallback);

        CompositeAnswerEvaluator evaluator = new CompositeAnswerEvaluator(llmAnswerEvaluator, ruleBasedAnswerEvaluator, properties);
        EvaluationResult result = evaluator.evaluate(context);
        assertEquals(60, result.score());
        assertEquals("rule", result.feedback());
    }
}

