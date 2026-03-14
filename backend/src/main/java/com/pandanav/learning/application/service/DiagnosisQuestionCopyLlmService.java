package com.pandanav.learning.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.pandanav.learning.application.service.llm.LlmJsonParser;
import com.pandanav.learning.domain.llm.LlmGateway;
import com.pandanav.learning.domain.llm.model.LlmCallContext;
import com.pandanav.learning.domain.llm.model.LlmCallMetrics;
import com.pandanav.learning.domain.llm.model.LlmInvocationProfile;
import com.pandanav.learning.domain.llm.model.LlmPrompt;
import com.pandanav.learning.domain.llm.model.LlmStage;
import com.pandanav.learning.domain.llm.model.LlmTextResult;
import com.pandanav.learning.domain.llm.model.PromptTemplateKey;
import com.pandanav.learning.domain.model.DiagnosisQuestion;
import com.pandanav.learning.domain.model.DiagnosisQuestionOption;
import com.pandanav.learning.domain.model.LearningSession;
import com.pandanav.learning.infrastructure.observability.LlmCallLogger;
import com.pandanav.learning.infrastructure.observability.LlmObservabilityHelper;
import com.pandanav.learning.infrastructure.exception.AiGenerationException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DiagnosisQuestionCopyLlmService {

    private final LlmGateway llmGateway;
    private final LlmJsonParser llmJsonParser;
    private final LlmCallLogger llmCallLogger;

    public DiagnosisQuestionCopyLlmService(
        LlmGateway llmGateway,
        LlmJsonParser llmJsonParser,
        LlmCallLogger llmCallLogger
    ) {
        this.llmGateway = llmGateway;
        this.llmJsonParser = llmJsonParser;
        this.llmCallLogger = llmCallLogger;
    }

    public List<DiagnosisQuestion> enhanceQuestions(LearningSession session, List<DiagnosisQuestion> sourceQuestions) {
        Instant start = Instant.now();
        try {
            LlmPrompt prompt = new LlmPrompt(
                PromptTemplateKey.DIAGNOSIS_QUESTION_V1,
                PromptTemplateKey.DIAGNOSIS_QUESTION_V1.promptKey(),
                PromptTemplateKey.DIAGNOSIS_QUESTION_V1.promptVersion(),
                LlmInvocationProfile.LIGHT_JSON_TASK,
                "你只负责润色诊断题目的中文文案，不得改变题目结构。只输出 JSON。",
                """
                    你正在为能力诊断题生成中文文案。
                    约束：
                    1. 只可输出 JSON，不得解释。
                    2. 不得新增字段，不得修改 questionId / dimension / type / required。
                    3. options 数量必须与输入一致，可润色 label 但不可增减。
                    4. title 不超过 18 字，description 不超过 40 字，placeholder 不超过 30 字。
                    5. 语气面向普通大学生，简洁、自然、非考试感。

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
                          "placeholder": "",
                          "submitHint": "",
                          "sectionLabel": "",
                          "options": [{"code": "BEGINNER", "label": ""}]
                        }
                      ]
                    }
                    """.formatted(
                    safe(session.getGoalText()),
                    safe(session.getCourseId()),
                    safe(session.getChapterId()),
                    serializeQuestions(sourceQuestions)
                ),
                "{\"questions\":[{\"questionId\":\"\",\"title\":\"\",\"description\":\"\",\"placeholder\":\"\",\"submitHint\":\"\",\"sectionLabel\":\"\",\"options\":[{\"code\":\"\",\"label\":\"\"}]}]}",
                "short_json_only",
                null,
                800
            );
            LlmCallContext startContext = LlmObservabilityHelper.context(LlmStage.DIAGNOSIS_QUESTION_COPY, null);
            llmCallLogger.logStart(startContext);
            LlmTextResult result = llmGateway.generate(LlmStage.DIAGNOSIS_QUESTION_COPY, prompt);
            LlmCallContext successContext = LlmObservabilityHelper.context(LlmStage.DIAGNOSIS_QUESTION_COPY, result.model());
            JsonNode root = llmJsonParser.parse(result.text());
            JsonNode items = root.path("questions");
            if (!items.isArray() || items.size() != sourceQuestions.size()) {
                throw new AiGenerationException("DIAGNOSIS_QUESTION_COPY", "MISSING_REQUIRED_FIELDS");
            }

            Map<String, DiagnosisQuestion> byId = sourceQuestions.stream()
                .collect(Collectors.toMap(DiagnosisQuestion::questionId, question -> question, (left, right) -> left, LinkedHashMap::new));

            List<DiagnosisQuestion> refined = new ArrayList<>();
            for (JsonNode item : items) {
                String questionId = item.path("questionId").asText("");
                DiagnosisQuestion original = byId.get(questionId);
                if (original == null) {
                    throw new AiGenerationException("DIAGNOSIS_QUESTION_COPY", "MISSING_REQUIRED_FIELDS");
                }
                List<DiagnosisQuestionOption> options = readOptions(item.path("options"), original.options());
                if (options.size() != original.options().size()) {
                    throw new AiGenerationException("DIAGNOSIS_QUESTION_COPY", "MISSING_REQUIRED_FIELDS");
                }
                refined.add(new DiagnosisQuestion(
                    original.questionId(),
                    original.dimension(),
                    original.type(),
                    original.required(),
                    options,
                    textOrDefault(item.path("title").asText(""), original.title()),
                    textOrDefault(item.path("description").asText(""), original.description()),
                    textOrDefault(item.path("placeholder").asText(""), original.placeholder()),
                    textOrDefault(item.path("submitHint").asText(""), original.submitHint()),
                    textOrDefault(item.path("sectionLabel").asText(""), original.sectionLabel())
                ));
            }
            validate(refined, sourceQuestions.size());
            llmCallLogger.logSuccess(
                successContext,
                toMetrics(result, start),
                result.usage() == null ? null : result.usage().finishReason(),
                result.usage() != null && result.usage().truncated()
            );
            return refined;
        } catch (AiGenerationException ex) {
            llmCallLogger.logFailure(
                LlmObservabilityHelper.context(LlmStage.DIAGNOSIS_QUESTION_COPY, null),
                com.pandanav.learning.domain.llm.model.LlmFailureType.UNKNOWN_ERROR,
                ex.getMessage(),
                LlmObservabilityHelper.elapsedMs(start)
            );
            throw ex;
        } catch (Exception ex) {
            if (isTimeout(ex)) {
                llmCallLogger.logFailure(
                    LlmObservabilityHelper.context(LlmStage.DIAGNOSIS_QUESTION_COPY, null),
                    com.pandanav.learning.domain.llm.model.LlmFailureType.TIMEOUT,
                    ex.getMessage(),
                    LlmObservabilityHelper.elapsedMs(start)
                );
                throw new AiGenerationException("DIAGNOSIS_QUESTION_COPY", "TIMEOUT");
            }
            llmCallLogger.logFailure(
                LlmObservabilityHelper.context(LlmStage.DIAGNOSIS_QUESTION_COPY, null),
                com.pandanav.learning.domain.llm.model.LlmFailureType.UNKNOWN_ERROR,
                ex.getMessage(),
                LlmObservabilityHelper.elapsedMs(start)
            );
            throw new AiGenerationException("DIAGNOSIS_QUESTION_COPY", "UNKNOWN_ERROR");
        }
    }

    private List<Map<String, Object>> serializeQuestions(List<DiagnosisQuestion> questions) {
        return questions.stream().map(question -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("questionId", question.questionId());
            item.put("dimension", question.dimension().name());
            item.put("type", question.type());
            item.put("required", question.required());
            item.put("title", question.title());
            item.put("description", question.description());
            item.put("placeholder", question.placeholder());
            item.put("submitHint", question.submitHint());
            item.put("sectionLabel", question.sectionLabel());
            item.put("options", question.options());
            return item;
        }).toList();
    }

    private List<DiagnosisQuestionOption> readOptions(JsonNode node, List<DiagnosisQuestionOption> fallback) {
        if (!node.isArray()) {
            return fallback;
        }
        List<DiagnosisQuestionOption> options = new ArrayList<>();
        for (int i = 0; i < node.size(); i++) {
            JsonNode item = node.get(i);
            DiagnosisQuestionOption fallbackOption = i < fallback.size() ? fallback.get(i) : null;
            String code = item.path("code").asText(fallbackOption == null ? "" : fallbackOption.code());
            String label = textOrDefault(item.path("label").asText(""), fallbackOption == null ? "" : fallbackOption.label());
            if (fallbackOption != null && !fallbackOption.code().equalsIgnoreCase(code)) {
                code = fallbackOption.code();
            }
            if (!code.isBlank() && !label.isBlank()) {
                options.add(new DiagnosisQuestionOption(code, label, fallbackOption == null ? i + 1 : fallbackOption.order()));
            }
        }
        return options.size() == fallback.size() ? options : fallback;
    }

    private String textOrDefault(String candidate, String fallback) {
        return candidate == null || candidate.isBlank() ? fallback : candidate.trim();
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
                || question.options() == null || question.options().isEmpty()) {
                throw new AiGenerationException("DIAGNOSIS_QUESTION_COPY", "EMPTY_CONTENT");
            }
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
