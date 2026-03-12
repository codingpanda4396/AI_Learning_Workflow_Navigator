package com.pandanav.learning.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.pandanav.learning.application.service.llm.LlmJsonParser;
import com.pandanav.learning.domain.llm.LlmGateway;
import com.pandanav.learning.domain.llm.model.LlmInvocationProfile;
import com.pandanav.learning.domain.llm.model.LlmPrompt;
import com.pandanav.learning.domain.llm.model.LlmTextResult;
import com.pandanav.learning.domain.llm.model.PromptTemplateKey;
import com.pandanav.learning.domain.model.DiagnosisQuestion;
import com.pandanav.learning.domain.model.DiagnosisQuestionCopy;
import com.pandanav.learning.domain.model.LearningSession;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DiagnosisQuestionCopyLlmService {

    private final LlmGateway llmGateway;
    private final LlmJsonParser llmJsonParser;

    public DiagnosisQuestionCopyLlmService(LlmGateway llmGateway, LlmJsonParser llmJsonParser) {
        this.llmGateway = llmGateway;
        this.llmJsonParser = llmJsonParser;
    }

    public List<DiagnosisQuestion> enhanceQuestions(LearningSession session, List<DiagnosisQuestion> fallbackQuestions) {
        try {
            LlmPrompt prompt = new LlmPrompt(
                PromptTemplateKey.DIAGNOSIS_QUESTION_V1,
                PromptTemplateKey.DIAGNOSIS_QUESTION_V1.promptKey(),
                PromptTemplateKey.DIAGNOSIS_QUESTION_V1.promptVersion(),
                LlmInvocationProfile.LIGHT_JSON_TASK,
                "你只负责润色诊断题目的中文文案，不得改变题目结构。只输出 JSON。",
                """
                    你正在为能力诊断题生成中文 copy。
                    约束：
                    1. 只可输出 JSON，不得解释。
                    2. 不得新增字段，不得修改 questionId / dimension / type / required。
                    3. options 数量必须与输入一致，可润色表达但不可增减。
                    4. 标题不超过28字，description 不超过40字，placeholder 不超过40字。
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
                          "copy": {
                            "sectionLabel": "KNOWLEDGE_FOUNDATION",
                            "title": "",
                            "description": "",
                            "placeholder": "",
                            "submitHint": ""
                          },
                          "options": ["", ""]
                        }
                      ]
                    }
                    """.formatted(
                    safe(session.getGoalText()),
                    safe(session.getCourseId()),
                    safe(session.getChapterId()),
                    serializeQuestions(fallbackQuestions)
                ),
                "{\"questions\":[{\"questionId\":\"\",\"copy\":{\"sectionLabel\":\"\",\"title\":\"\",\"description\":\"\",\"placeholder\":\"\",\"submitHint\":\"\"},\"options\":[\"\"]}]}",
                "short_json_only",
                null,
                800
            );
            LlmTextResult result = llmGateway.generate(prompt);
            JsonNode root = llmJsonParser.parse(result.text());
            JsonNode items = root.path("questions");
            if (!items.isArray() || items.size() != fallbackQuestions.size()) {
                return fallbackQuestions;
            }

            Map<String, DiagnosisQuestion> byId = fallbackQuestions.stream()
                .collect(Collectors.toMap(DiagnosisQuestion::questionId, question -> question, (left, right) -> left, LinkedHashMap::new));

            List<DiagnosisQuestion> refined = new ArrayList<>();
            for (JsonNode item : items) {
                String questionId = item.path("questionId").asText("");
                DiagnosisQuestion original = byId.get(questionId);
                if (original == null) {
                    return fallbackQuestions;
                }
                List<String> options = readOptions(item.path("options"), original.options());
                if (options.size() != original.options().size()) {
                    return fallbackQuestions;
                }
                DiagnosisQuestionCopy copy = readCopy(item.path("copy"), original.copy());
                refined.add(new DiagnosisQuestion(
                    original.questionId(),
                    original.dimension(),
                    original.type(),
                    copy.title(),
                    copy.description(),
                    options,
                    original.required(),
                    copy
                ));
            }
            return refined;
        } catch (Exception ex) {
            return fallbackQuestions;
        }
    }

    private List<Map<String, Object>> serializeQuestions(List<DiagnosisQuestion> questions) {
        return questions.stream().map(question -> {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("questionId", question.questionId());
            item.put("dimension", question.dimension().name());
            item.put("type", question.type());
            item.put("required", question.required());
            item.put("copy", question.copy());
            item.put("options", question.options());
            return item;
        }).toList();
    }

    private List<String> readOptions(JsonNode node, List<String> fallback) {
        if (!node.isArray()) {
            return fallback;
        }
        List<String> options = new ArrayList<>();
        for (JsonNode item : node) {
            String text = item.asText("").trim();
            if (!text.isBlank()) {
                options.add(text);
            }
        }
        return options.size() == fallback.size() ? options : fallback;
    }

    private DiagnosisQuestionCopy readCopy(JsonNode node, DiagnosisQuestionCopy fallback) {
        if (!node.isObject()) {
            return fallback;
        }
        return new DiagnosisQuestionCopy(
            textOrDefault(node.path("sectionLabel").asText(""), fallback.sectionLabel()),
            textOrDefault(node.path("title").asText(""), fallback.title()),
            textOrDefault(node.path("description").asText(""), fallback.description()),
            textOrDefault(node.path("placeholder").asText(""), fallback.placeholder()),
            textOrDefault(node.path("submitHint").asText(""), fallback.submitHint())
        );
    }

    private String textOrDefault(String candidate, String fallback) {
        return candidate == null || candidate.isBlank() ? fallback : candidate.trim();
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "" : value.trim();
    }
}
