package com.pandanav.learning.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.pandanav.learning.application.service.llm.LlmJsonParser;
import com.pandanav.learning.domain.enums.DiagnosisDimension;
import com.pandanav.learning.domain.llm.LlmGateway;
import com.pandanav.learning.domain.llm.model.LlmFallbackReason;
import com.pandanav.learning.domain.llm.model.LlmInvocationProfile;
import com.pandanav.learning.domain.llm.model.LlmPrompt;
import com.pandanav.learning.domain.llm.model.LlmStage;
import com.pandanav.learning.domain.llm.model.LlmTextResult;
import com.pandanav.learning.domain.llm.model.PromptTemplateKey;
import com.pandanav.learning.domain.model.CapabilityProfileDraft;
import com.pandanav.learning.domain.model.CapabilityProfileSummaryCopy;
import com.pandanav.learning.domain.model.LearningSession;
import com.pandanav.learning.infrastructure.observability.LlmCallLogger;
import com.pandanav.learning.infrastructure.observability.LlmFailureClassifier;
import com.pandanav.learning.infrastructure.observability.LlmObservabilityHelper;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CapabilityProfileSummaryLlmService {

    private final LlmGateway llmGateway;
    private final LlmJsonParser llmJsonParser;
    private final LlmCallLogger llmCallLogger;
    private final LlmFailureClassifier llmFailureClassifier;

    public CapabilityProfileSummaryLlmService(
        LlmGateway llmGateway,
        LlmJsonParser llmJsonParser,
        LlmCallLogger llmCallLogger,
        LlmFailureClassifier llmFailureClassifier
    ) {
        this.llmGateway = llmGateway;
        this.llmJsonParser = llmJsonParser;
        this.llmCallLogger = llmCallLogger;
        this.llmFailureClassifier = llmFailureClassifier;
    }

    public CapabilityProfileSummaryResult generate(
        LearningSession session,
        CapabilityProfileDraft draft,
        Map<DiagnosisDimension, List<String>> answersByDimension
    ) {
        Instant start = Instant.now();
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
            LlmTextResult result = llmGateway.generate(LlmStage.CAPABILITY_SUMMARY, prompt);
            JsonNode root = llmJsonParser.parse(result.text());
            String summary = root.path("summary").asText("").trim();
            String planExplanation = root.path("planExplanation").asText("").trim();
            if (summary.isBlank() || planExplanation.isBlank()) {
                llmCallLogger.logFallback(
                    LlmObservabilityHelper.context(LlmStage.CAPABILITY_SUMMARY, result.model()),
                    LlmFallbackReason.MISSING_REQUIRED_FIELDS,
                    LlmObservabilityHelper.elapsedMs(start)
                );
                return new CapabilityProfileSummaryResult(null, true, List.of(LlmFallbackReason.MISSING_REQUIRED_FIELDS.name()));
            }
            return new CapabilityProfileSummaryResult(
                new CapabilityProfileSummaryCopy(summary, planExplanation),
                false,
                List.of()
            );
        } catch (Exception ex) {
            llmCallLogger.logFallback(
                LlmObservabilityHelper.context(LlmStage.CAPABILITY_SUMMARY, null),
                llmFailureClassifier.classifyFallback(ex),
                LlmObservabilityHelper.elapsedMs(start)
            );
            return new CapabilityProfileSummaryResult(
                null,
                true,
                List.of(llmFailureClassifier.classifyFallback(ex).name())
            );
        }
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "" : value.trim();
    }

    public record CapabilityProfileSummaryResult(
        CapabilityProfileSummaryCopy copy,
        boolean fallbackApplied,
        List<String> fallbackReasons
    ) {
    }
}
