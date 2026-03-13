package com.pandanav.learning.application.service.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pandanav.learning.domain.llm.LlmGateway;
import com.pandanav.learning.domain.llm.PromptTemplateProvider;
import com.pandanav.learning.domain.llm.model.EvaluationContext;
import com.pandanav.learning.domain.llm.model.LlmInvocationProfile;
import com.pandanav.learning.domain.llm.model.LlmPrompt;
import com.pandanav.learning.domain.llm.model.LlmStage;
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
            new LlmPrompt(PromptTemplateKey.EVALUATE_V1, "EVALUATE", "v1", LlmInvocationProfile.LIGHT_JSON_TASK, "sys", "user", "{}", "", null, null)
        );
        when(gateway.generate(any(LlmStage.class), any())).thenReturn(
            new LlmTextResult(
                """
                    {
                      "score":84,
                      "normalized_score":0.84,
                      "rubric":{"concept_correctness":34,"reasoning_quality":25,"completeness":17,"clarity":8},
                      "feedback":"答案核心概念基本正确，但推理链路还可以更完整。",
                      "error_tags":["MISSING_STEPS","TERMINOLOGY"],
                      "strengths":["s1","s2"],
                      "weaknesses":["w1","w2"],
                      "suggested_next_action":"INSERT_TRAINING_REINFORCEMENT"
                    }
                    """,
                "openai-compatible",
                "gpt-4o-mini",
                LlmInvocationProfile.LIGHT_JSON_TASK,
                new LlmUsage(10, 20, 30, 0, 100, "stop", false, false),
                new ObjectMapper().createObjectNode(),
                new ObjectMapper().createObjectNode()
            )
        );

        LlmAnswerEvaluator evaluator = new LlmAnswerEvaluator(gateway, provider, parser, new PromptOutputValidator());
        EvaluationContext context = new EvaluationContext(1L, 1L, 1L, "obj", "q", "a", null, Stage.TRAINING);
        var result = evaluator.evaluate(context);
        assertEquals(84, result.score());
        assertEquals(new BigDecimal("0.840"), result.normalizedScore());
        assertEquals(List.of("MISSING_STEPS", "TERMINOLOGY"), result.errorTags());
    }
}
