package com.pandanav.learning.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.pandanav.learning.application.service.llm.LlmJsonParser;
import com.pandanav.learning.application.service.llm.LlmJsonParseException;
import com.pandanav.learning.domain.llm.LlmGateway;
import com.pandanav.learning.domain.llm.model.LlmCallContext;
import com.pandanav.learning.domain.llm.model.LlmCallMetrics;
import com.pandanav.learning.domain.llm.model.LlmFailureType;
import com.pandanav.learning.domain.llm.model.LlmFallbackReason;
import com.pandanav.learning.domain.llm.model.LlmInvocationProfile;
import com.pandanav.learning.domain.llm.model.LlmPrompt;
import com.pandanav.learning.domain.llm.model.LlmStage;
import com.pandanav.learning.domain.llm.model.LlmTextResult;
import com.pandanav.learning.domain.llm.model.PromptTemplateKey;
import com.pandanav.learning.domain.model.DiagnosisQuestion;
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
public class DiagnosisQuestionCopyLlmService {

    private final LlmGateway llmGateway;
    private final LlmJsonParser llmJsonParser;
    private final LlmCallLogger llmCallLogger;
    private final QuestionStructureAssembler questionStructureAssembler;

    private static final int EXPECTED_QUESTION_COUNT = 5;

    public DiagnosisQuestionCopyLlmService(
        LlmGateway llmGateway,
        LlmJsonParser llmJsonParser,
        LlmCallLogger llmCallLogger,
        QuestionStructureAssembler questionStructureAssembler
    ) {
        this.llmGateway = llmGateway;
        this.llmJsonParser = llmJsonParser;
        this.llmCallLogger = llmCallLogger;
        this.questionStructureAssembler = questionStructureAssembler;
    }

    public List<DiagnosisQuestion> enhanceQuestions(LearningSession session, List<DiagnosisQuestion> sourceQuestions) {
        Instant start = Instant.now();
        try {
            LlmPrompt prompt = new LlmPrompt(
                PromptTemplateKey.DIAGNOSIS_QUESTION_V1,
                PromptTemplateKey.DIAGNOSIS_QUESTION_V1.promptKey(),
                PromptTemplateKey.DIAGNOSIS_QUESTION_V1.promptVersion(),
                LlmInvocationProfile.LIGHT_JSON_TASK,
                "你只负责生成诊断题中文文案。只返回 JSON 对象，不得输出任何解释。",
                """
                    你正在为能力诊断题生成中文文案。
                    约束：
                    1. 必须只返回 JSON；不要解释、不要 markdown、不要代码块、不要多余文本。
                    2. 输出必须从 { 开始，到 } 结束。
                    3. 必须生成 exactly 5 个问题，questions 数组长度必须是 5。
                    4. 只允许输出 questionId/title/description/submitHint/sectionLabel 这 5 个字段。
                    5. 严格限制长度：title<=20字，description<=40字，submitHint<=30字。
                    6. 不要输出 options、dimension、type、required、placeholder。
                    7. 语气面向普通大学生，简洁、自然、非考试感。

                    输入：
                    {
                      "goal": "%s",
                      "course": "%s",
                      "chapter": "%s",
                      "questions": %s
                    }

                    输出格式：
                    {
                      "questions": [
                        {
                          "questionId": "q_foundation",
                          "title": "",
                          "description": "",
                          "submitHint": "",
                          "sectionLabel": ""
                        }
                      ]
                    }
                    """.formatted(
                    safe(session.getGoalText()),
                    safe(session.getCourseId()),
                    safe(session.getChapterId()),
                    serializeQuestions(sourceQuestions)
                ),
                "{\"questions\":[{\"questionId\":\"\",\"title\":\"\",\"description\":\"\",\"submitHint\":\"\",\"sectionLabel\":\"\"}]}",
                "short_json_only",
                null,
                600
            );
            LlmCallContext startContext = LlmObservabilityHelper.context(LlmStage.DIAGNOSIS_QUESTION_COPY, null);
            llmCallLogger.logStart(startContext);
            LlmTextResult result = llmGateway.generate(LlmStage.DIAGNOSIS_QUESTION_COPY, prompt);
            LlmCallContext successContext = LlmObservabilityHelper.context(LlmStage.DIAGNOSIS_QUESTION_COPY, result.model());
            llmCallLogger.logSuccess(
                successContext,
                toMetrics(result, start),
                result.usage() == null ? null : result.usage().finishReason(),
                result.usage() != null && result.usage().truncated()
            );
            llmCallLogger.logOutputSize(
                successContext,
                result.text() == null ? 0 : result.text().length(),
                result.usage() == null ? null : result.usage().tokenOutput(),
                result.usage() == null ? null : result.usage().finishReason(),
                result.usage() != null && result.usage().truncated()
            );
            if (isTruncated(result)) {
                llmCallLogger.logFailure(
                    successContext,
                    LlmFailureType.LLM_TRUNCATED,
                    "finishReason=%s truncated=%s".formatted(
                        result.usage() == null ? "unknown" : result.usage().finishReason(),
                        result.usage() != null && result.usage().truncated()
                    ),
                    LlmObservabilityHelper.elapsedMs(start)
                );
                throw new AiGenerationException("DIAGNOSIS_QUESTION_COPY", "OUTPUT_TRUNCATED");
            }

            JsonNode root = llmJsonParser.parse(
                result.text(),
                successContext.stage().name(),
                successContext.model(),
                successContext.traceId(),
                successContext.requestId()
            );
            JsonNode items = root.path("questions");
            if (!items.isArray() || items.size() != EXPECTED_QUESTION_COUNT) {
                throw new AiGenerationException("DIAGNOSIS_QUESTION_COPY", "MISSING_REQUIRED_FIELDS");
            }
            List<DiagnosisQuestion> refined = questionStructureAssembler.assemble(sourceQuestions, items);
            validate(refined, EXPECTED_QUESTION_COUNT);
            return refined;
        } catch (LlmJsonParseException ex) {
            LlmFailureType failureType = ex.fallbackReason() == LlmFallbackReason.OUTPUT_TRUNCATED
                ? LlmFailureType.LLM_TRUNCATED
                : LlmFailureType.LLM_JSON_PARSE_ERROR;
            llmCallLogger.logFailure(
                LlmObservabilityHelper.context(LlmStage.DIAGNOSIS_QUESTION_COPY, null),
                failureType,
                ex.diagnosticSummary(),
                LlmObservabilityHelper.elapsedMs(start)
            );
            throw new AiGenerationException("DIAGNOSIS_QUESTION_COPY", "JSON_PARSE_FAILED");
        } catch (AiGenerationException ex) {
            LlmFailureType failureType = "OUTPUT_TRUNCATED".equalsIgnoreCase(ex.getReason())
                ? LlmFailureType.LLM_TRUNCATED
                : LlmFailureType.UNKNOWN_ERROR;
            llmCallLogger.logFailure(
                LlmObservabilityHelper.context(LlmStage.DIAGNOSIS_QUESTION_COPY, null),
                failureType,
                ex.getMessage(),
                LlmObservabilityHelper.elapsedMs(start)
            );
            throw ex;
        } catch (Exception ex) {
            if (isTimeout(ex)) {
                llmCallLogger.logFailure(
                    LlmObservabilityHelper.context(LlmStage.DIAGNOSIS_QUESTION_COPY, null),
                    LlmFailureType.LLM_TIMEOUT,
                    ex.getMessage(),
                    LlmObservabilityHelper.elapsedMs(start)
                );
                throw new AiGenerationException("DIAGNOSIS_QUESTION_COPY", "TIMEOUT");
            }
            llmCallLogger.logFailure(
                LlmObservabilityHelper.context(LlmStage.DIAGNOSIS_QUESTION_COPY, null),
                LlmFailureType.LLM_API_ERROR,
                ex.getMessage(),
                LlmObservabilityHelper.elapsedMs(start)
            );
            throw new AiGenerationException("DIAGNOSIS_QUESTION_COPY", "API_ERROR");
        }
    }

    private List<Map<String, Object>> serializeQuestions(List<DiagnosisQuestion> questions) {
        return questions.stream().map(question -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("questionId", question.questionId());
            item.put("title", question.title());
            item.put("description", question.description());
            item.put("submitHint", question.submitHint());
            item.put("sectionLabel", question.sectionLabel());
            return item;
        }).toList();
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "" : value.trim();
    }

    private void validate(List<DiagnosisQuestion> questions, int expectedCount) {
        if (questions == null) {
            throw new AiGenerationException("DIAGNOSIS_QUESTION_COPY", "NULL_RESULT");
        }
        if (questions.size() != expectedCount) {
            throw new AiGenerationException("DIAGNOSIS_QUESTION_COPY", "MISSING_REQUIRED_FIELDS");
        }
        for (DiagnosisQuestion question : questions) {
            if (question == null
                || question.questionId() == null || question.questionId().isBlank()
                || question.title() == null || question.title().isBlank()
                || question.description() == null || question.description().isBlank()
                || question.options() == null || question.options().isEmpty()) {
                throw new AiGenerationException("DIAGNOSIS_QUESTION_COPY", "EMPTY_CONTENT");
            }
        }
    }

    private boolean isTruncated(LlmTextResult result) {
        if (result == null || result.usage() == null) {
            return false;
        }
        return result.usage().truncated() || "length".equalsIgnoreCase(result.usage().finishReason());
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
