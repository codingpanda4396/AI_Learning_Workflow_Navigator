package com.pandanav.learning.application.service;

import com.pandanav.learning.api.dto.session.GoalDiagnosisRequest;
import com.pandanav.learning.application.service.llm.LlmJsonParser;
import com.pandanav.learning.application.service.llm.PromptOutputValidator;
import com.pandanav.learning.domain.llm.LlmGateway;
import com.pandanav.learning.domain.llm.PromptTemplateProvider;
import com.pandanav.learning.domain.llm.model.LlmPrompt;
import com.pandanav.learning.domain.llm.model.LlmTextResult;
import com.pandanav.learning.domain.llm.model.PromptTemplateKey;
import com.pandanav.learning.infrastructure.config.LlmProperties;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GoalDiagnosisServiceTest {

    @Test
    void shouldParseSmartBreakdownFromLlm() {
        LlmGateway gateway = mock(LlmGateway.class);
        PromptTemplateProvider provider = mock(PromptTemplateProvider.class);

        when(provider.buildGoalDiagnosisPrompt(any())).thenReturn(
            new LlmPrompt(PromptTemplateKey.GOAL_DIAGNOSE_V1, "GOAL_DIAGNOSE", "v1", "sys", "user", "{}", "", null)
        );
        when(gateway.generate(any())).thenReturn(
            new LlmTextResult(
                """
                    {
                      "goal_score":80,
                      "smart_breakdown":{
                        "specific_score":16,
                        "measurable_score":16,
                        "achievable_score":16,
                        "relevant_score":16,
                        "time_bound_score":16
                      },
                      "summary":"目标较清晰，可执行性较好。",
                      "strengths":["范围明确","有量化指标"],
                      "risks":["时间节点略宽泛","验收标准还可细化"],
                      "rewritten_goal":"在 7 天内完成 3 次训练并保持平均分不低于 80 分。"
                    }
                    """,
                "provider",
                "model",
                null,
                null,
                null
            )
        );

        LlmProperties properties = new LlmProperties();
        properties.setEnabled(true);
        properties.setBaseUrl("http://localhost");
        properties.setApiKey("k");
        properties.setModel("m");

        GoalDiagnosisService service = new GoalDiagnosisService(
            gateway,
            properties,
            provider,
            new LlmJsonParser(new com.fasterxml.jackson.databind.ObjectMapper()),
            new PromptOutputValidator()
        );

        var result = service.diagnose(new GoalDiagnosisRequest("c1", "ch1", "一周内掌握 TCP 连接建立"));
        assertEquals(80, result.goalScore());
        assertEquals(16, result.smartBreakdown().specificScore());
    }
}
