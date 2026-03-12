package com.pandanav.learning.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.pandanav.learning.application.service.llm.LlmJsonParser;
import com.pandanav.learning.domain.enums.DiagnosisDimension;
import com.pandanav.learning.domain.llm.LlmGateway;
import com.pandanav.learning.domain.llm.model.LlmInvocationProfile;
import com.pandanav.learning.domain.llm.model.LlmPrompt;
import com.pandanav.learning.domain.llm.model.LlmTextResult;
import com.pandanav.learning.domain.llm.model.PromptTemplateKey;
import com.pandanav.learning.domain.model.CapabilityProfileDraft;
import com.pandanav.learning.domain.model.CapabilityProfileSummaryCopy;
import com.pandanav.learning.domain.model.LearningSession;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class CapabilityProfileSummaryLlmService {

    private final LlmGateway llmGateway;
    private final LlmJsonParser llmJsonParser;

    public CapabilityProfileSummaryLlmService(LlmGateway llmGateway, LlmJsonParser llmJsonParser) {
        this.llmGateway = llmGateway;
        this.llmJsonParser = llmJsonParser;
    }

    public Optional<CapabilityProfileSummaryCopy> generate(
        LearningSession session,
        CapabilityProfileDraft draft,
        Map<DiagnosisDimension, List<String>> answersByDimension
    ) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("goal", safe(session.getGoalText()));
            payload.put("course", safe(session.getCourseId()));
            payload.put("chapter", safe(session.getChapterId()));
            payload.put("answers", answersByDimension);
            payload.put("profileDraft", draft);

            LlmPrompt prompt = new LlmPrompt(
                PromptTemplateKey.CAPABILITY_SUMMARY_V1,
                PromptTemplateKey.CAPABILITY_SUMMARY_V1.promptKey(),
                PromptTemplateKey.CAPABILITY_SUMMARY_V1.promptVersion(),
                LlmInvocationProfile.LIGHT_JSON_TASK,
                "你只负责把结构化能力画像草稿翻译成自然语言总结。只输出 JSON。",
                """
                    你正在生成能力诊断结果文案。
                    约束：
                    1. 只输出 JSON，不得解释。
                    2. 只根据输入生成，不得新增业务判断，不得捏造用户未表达的信息。
                    3. 面向普通大学生，语气自然，不夸张。
                    4. summary 和 planExplanation 各 1-2 句。
                    
                    输入：
                    %s
                    
                    输出格式：
                    {
                      "summary": "",
                      "planExplanation": ""
                    }
                    """.formatted(payload),
                "{\"summary\":\"\",\"planExplanation\":\"\"}",
                "short_json_only",
                null,
                400
            );
            LlmTextResult result = llmGateway.generate(prompt);
            JsonNode root = llmJsonParser.parse(result.text());
            String summary = root.path("summary").asText("").trim();
            String planExplanation = root.path("planExplanation").asText("").trim();
            if (summary.isBlank() || planExplanation.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(new CapabilityProfileSummaryCopy(summary, planExplanation));
        } catch (Exception ex) {
            return Optional.empty();
        }
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "" : value.trim();
    }
}
