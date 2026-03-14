package com.pandanav.learning.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.pandanav.learning.application.service.llm.LlmJsonParser;
import com.pandanav.learning.domain.enums.DiagnosisDimension;
import com.pandanav.learning.domain.llm.LlmGateway;
import com.pandanav.learning.domain.llm.model.LlmCallContext;
import com.pandanav.learning.domain.llm.model.LlmCallMetrics;
import com.pandanav.learning.domain.llm.model.LlmInvocationProfile;
import com.pandanav.learning.domain.llm.model.LlmPrompt;
import com.pandanav.learning.domain.llm.model.LlmStage;
import com.pandanav.learning.domain.llm.model.LlmTextResult;
import com.pandanav.learning.domain.llm.model.PromptTemplateKey;
import com.pandanav.learning.domain.model.CapabilityProfileDraft;
import com.pandanav.learning.domain.model.CapabilityProfileSummaryCopy;
import com.pandanav.learning.domain.model.LearningSession;
import com.pandanav.learning.infrastructure.observability.LlmCallLogger;
import com.pandanav.learning.infrastructure.observability.LlmObservabilityHelper;
import com.pandanav.learning.infrastructure.exception.AiGenerationException;
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

    public CapabilityProfileSummaryLlmService(
        LlmGateway llmGateway,
        LlmJsonParser llmJsonParser,
        LlmCallLogger llmCallLogger
    ) {
        this.llmGateway = llmGateway;
        this.llmJsonParser = llmJsonParser;
        this.llmCallLogger = llmCallLogger;
    }

    public CapabilityProfileSummaryCopy generate(
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
            LlmCallContext startContext = LlmObservabilityHelper.context(LlmStage.CAPABILITY_SUMMARY, null);
            llmCallLogger.logStart(startContext);
            LlmTextResult result = llmGateway.generate(LlmStage.CAPABILITY_SUMMARY, prompt);
            LlmCallContext successContext = LlmObservabilityHelper.context(LlmStage.CAPABILITY_SUMMARY, result.model());
            JsonNode root = llmJsonParser.parse(result.text());
            String summary = root.path("summary").asText("").trim();
            String planExplanation = root.path("planExplanation").asText("").trim();
            CapabilityProfileSummaryCopy copy = new CapabilityProfileSummaryCopy(summary, planExplanation);
            validate(copy);
            llmCallLogger.logSuccess(
                successContext,
                toMetrics(result, start),
                result.usage() == null ? null : result.usage().finishReason(),
                result.usage() != null && result.usage().truncated()
            );
            return copy;
        } catch (AiGenerationException ex) {
            llmCallLogger.logFailure(
                LlmObservabilityHelper.context(LlmStage.CAPABILITY_SUMMARY, null),
                com.pandanav.learning.domain.llm.model.LlmFailureType.UNKNOWN_ERROR,
                ex.getMessage(),
                LlmObservabilityHelper.elapsedMs(start)
            );
            throw ex;
        } catch (Exception ex) {
            if (isTimeout(ex)) {
                llmCallLogger.logFailure(
                    LlmObservabilityHelper.context(LlmStage.CAPABILITY_SUMMARY, null),
                    com.pandanav.learning.domain.llm.model.LlmFailureType.TIMEOUT,
                    ex.getMessage(),
                    LlmObservabilityHelper.elapsedMs(start)
                );
                throw new AiGenerationException("CAPABILITY_PROFILE_SUMMARY", "TIMEOUT");
            }
            llmCallLogger.logFailure(
                LlmObservabilityHelper.context(LlmStage.CAPABILITY_SUMMARY, null),
                com.pandanav.learning.domain.llm.model.LlmFailureType.UNKNOWN_ERROR,
                ex.getMessage(),
                LlmObservabilityHelper.elapsedMs(start)
            );
            throw new AiGenerationException("CAPABILITY_PROFILE_SUMMARY", "UNKNOWN_ERROR");
        }
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "" : value.trim();
    }

    private void validate(CapabilityProfileSummaryCopy summary) {
        if (summary == null) {
            throw new AiGenerationException("CAPABILITY_PROFILE_SUMMARY", "NULL_RESULT");
        }
        if (summary.summary() == null || summary.summary().isBlank()) {
            throw new AiGenerationException("CAPABILITY_PROFILE_SUMMARY", "EMPTY_CONTENT");
        }
        if (summary.planExplanation() == null || summary.planExplanation().isBlank()) {
            throw new AiGenerationException("CAPABILITY_PROFILE_SUMMARY", "MISSING_REQUIRED_FIELDS");
        }
    }

    private LlmCallMetrics toMetrics(LlmTextResult result, Instant start) {
        if (result == null || result.usage() == null) {
            return new LlmCallMetrics(LlmObservabilityHelper.elapsedMs(start), -1, -1, -1);
        }
        return new LlmCallMetrics(
            LlmObservabilityHelper.elapsedMs(start),
            result.usage().tokenInput() == null ? -1 : result.usage().tokenInput(),
            result.usage().tokenOutput() == null ? -1 : result.usage().tokenOutput(),
            result.usage().totalTokens() == null ? -1 : result.usage().totalTokens()
        );
    }

    private boolean isTimeout(Exception ex) {
        String message = ex.getMessage() == null ? "" : ex.getMessage().toLowerCase();
        return message.contains("timeout")
            || message.contains("timed out")
            || ex.getClass().getSimpleName().toLowerCase().contains("timeout");
    }
}
