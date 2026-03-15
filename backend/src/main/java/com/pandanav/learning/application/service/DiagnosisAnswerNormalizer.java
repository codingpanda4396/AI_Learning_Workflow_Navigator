package com.pandanav.learning.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pandanav.learning.api.contract.ContractCatalog;
import com.pandanav.learning.domain.model.DiagnosisAnswer;
import com.pandanav.learning.domain.model.DiagnosisQuestion;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class DiagnosisAnswerNormalizer {

    private final ObjectMapper objectMapper;

    public DiagnosisAnswerNormalizer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<NormalizedDiagnosisAnswer> normalize(List<DiagnosisQuestion> questions, List<DiagnosisAnswer> answers) {
        Map<String, DiagnosisQuestion> questionById = new LinkedHashMap<>();
        for (DiagnosisQuestion question : questions) {
            questionById.put(question.questionId(), question);
        }

        List<NormalizedDiagnosisAnswer> normalizedAnswers = new ArrayList<>();
        for (DiagnosisAnswer answer : answers) {
            DiagnosisQuestion question = questionById.get(answer.getQuestionId());
            if (question == null) {
                continue;
            }
            JsonNode valueNode = readJson(answer.getAnswerValueJson());
            String answerType = ContractCatalog.diagnosisQuestionTypeCode(question.type());

            if ("TEXT".equals(answerType)) {
                normalizedAnswers.add(new NormalizedDiagnosisAnswer(
                    answer.getDiagnosisSessionId(),
                    answer.getQuestionId(),
                    answerType,
                    List.of(),
                    valueNode == null ? safe(answer.getRawText()) : valueNode.asText(""),
                    valueNode
                ));
                continue;
            }

            List<String> optionCodes = normalizeOptionCodes(question, valueNode, answer.getRawText(), "MULTIPLE_CHOICE".equals(answerType));
            normalizedAnswers.add(new NormalizedDiagnosisAnswer(
                answer.getDiagnosisSessionId(),
                answer.getQuestionId(),
                answerType,
                optionCodes,
                null,
                valueNode
            ));
        }
        return normalizedAnswers;
    }

    private JsonNode readJson(String json) {
        if (json == null || json.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(json);
        } catch (Exception ex) {
            return null;
        }
    }

    private List<String> normalizeOptionCodes(
        DiagnosisQuestion question,
        JsonNode valueNode,
        String rawText,
        boolean multipleChoice
    ) {
        List<String> values = new ArrayList<>();
        if (valueNode != null) {
            if (valueNode.isArray()) {
                for (JsonNode node : valueNode) {
                    addOptionCode(values, question, node.asText(""));
                }
            } else {
                addOptionCode(values, question, valueNode.asText(""));
            }
        }
        if (values.isEmpty() && rawText != null && !rawText.isBlank()) {
            for (String item : rawText.split("\\|")) {
                addOptionCode(values, question, item.trim());
            }
        }
        List<String> deduplicated = values.stream().distinct().toList();
        if (!multipleChoice && !deduplicated.isEmpty()) {
            return List.of(deduplicated.get(0));
        }
        return deduplicated;
    }

    private void addOptionCode(List<String> values, DiagnosisQuestion question, String raw) {
        String code = ContractCatalog.diagnosisOptionCode(question.dimension(), safe(raw));
        if (!code.isBlank()) {
            values.add(code);
        }
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    public record NormalizedDiagnosisAnswer(
        Long diagnosisSessionId,
        String questionId,
        String answerType,
        List<String> selectedOptionCodes,
        String textAnswer,
        JsonNode rawValue
    ) {
    }
}
