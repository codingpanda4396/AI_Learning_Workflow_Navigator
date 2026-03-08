package com.pandanav.learning.application.service.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pandanav.learning.domain.llm.LlmGateway;
import com.pandanav.learning.domain.llm.PromptTemplateProvider;
import com.pandanav.learning.domain.llm.model.EvaluationContext;
import com.pandanav.learning.domain.llm.model.LlmPrompt;
import com.pandanav.learning.domain.llm.model.LlmTextResult;
import com.pandanav.learning.domain.llm.model.LlmUsage;
import com.pandanav.learning.domain.llm.model.PromptTemplateKey;
import com.pandanav.learning.domain.enums.Stage;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LlmAnswerEvaluatorTest {

    @Test
    void shouldParseLlmEvaluationJson() {
        LlmGateway gateway = mock(LlmGateway.class);
        PromptTemplateProvider provider = mock(PromptTemplateProvider.class);
        LlmJsonParser parser = new LlmJsonParser(new ObjectMapper());

        when(provider.buildEvaluationPrompt(any(), any())).thenReturn(
            new LlmPrompt(PromptTemplateKey.EVALUATE_PROMPT_V1, "v1", "sys", "user")
        );
        when(gateway.generate(any())).thenReturn(
            new LlmTextResult(
                """
                    {"score":84,"normalized_score":0.84,"feedback":"ok","error_tags":["MISSING_STEPS"],"strengths":["s"],"weaknesses":["w"],"suggested_next_action":"INSERT_TRAINING_REINFORCEMENT"}
                    """,
                "openai-compatible",
                "gpt-4o-mini",
                new LlmUsage(10, 20, 100),
                new ObjectMapper().createObjectNode(),
                new ObjectMapper().createObjectNode()
            )
        );

        LlmAnswerEvaluator evaluator = new LlmAnswerEvaluator(gateway, provider, parser);
        EvaluationContext context = new EvaluationContext(1L, 1L, 1L, "obj", "q", "a", null, Stage.TRAINING);
        var result = evaluator.evaluate(context);
        assertEquals(84, result.score());
        assertEquals(new BigDecimal("0.840"), result.normalizedScore());
        assertEquals(List.of("MISSING_STEPS"), result.errorTags());
    }
}
