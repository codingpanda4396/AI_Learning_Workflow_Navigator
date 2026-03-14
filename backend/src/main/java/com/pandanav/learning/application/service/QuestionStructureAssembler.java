package com.pandanav.learning.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.pandanav.learning.domain.model.DiagnosisQuestion;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class QuestionStructureAssembler {

    public List<DiagnosisQuestion> assemble(List<DiagnosisQuestion> sourceQuestions, JsonNode llmQuestions) {
        Map<String, JsonNode> llmById = new LinkedHashMap<>();
        if (llmQuestions != null && llmQuestions.isArray()) {
            for (JsonNode item : llmQuestions) {
                String questionId = item.path("questionId").asText("").trim();
                if (!questionId.isBlank()) {
                    llmById.put(questionId, item);
                }
            }
        }

        List<DiagnosisQuestion> assembled = new ArrayList<>();
        for (DiagnosisQuestion original : sourceQuestions) {
            JsonNode llmItem = llmById.get(original.questionId());
            String title = textOrDefault(text(llmItem, "title"), original.title());
            String description = textOrDefault(text(llmItem, "description"), original.description());
            String submitHint = textOrDefault(text(llmItem, "submitHint"), original.submitHint());
            String sectionLabel = textOrDefault(text(llmItem, "sectionLabel"), original.sectionLabel());
            assembled.add(new DiagnosisQuestion(
                original.questionId(),
                original.dimension(),
                original.type(),
                original.required(),
                original.options(),
                title,
                description,
                original.placeholder(),
                submitHint,
                sectionLabel
            ));
        }
        return assembled;
    }

    private String text(JsonNode node, String field) {
        if (node == null) {
            return "";
        }
        return node.path(field).asText("");
    }

    private String textOrDefault(String candidate, String fallback) {
        return candidate == null || candidate.isBlank() ? fallback : candidate.trim();
    }
}
