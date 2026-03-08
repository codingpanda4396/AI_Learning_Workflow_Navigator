package com.pandanav.learning.application.service.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pandanav.learning.infrastructure.exception.InternalServerException;
import org.springframework.stereotype.Component;

@Component
public class LlmJsonParser {

    private final ObjectMapper objectMapper;

    public LlmJsonParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public JsonNode parse(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            throw new InternalServerException("LLM returned empty content.");
        }

        String cleaned = rawText.trim()
            .replace("```json", "")
            .replace("```", "")
            .trim();

        try {
            return objectMapper.readTree(cleaned);
        } catch (Exception ignored) {
            String extracted = extractJsonObject(cleaned);
            if (extracted == null) {
                throw new InternalServerException("Failed to parse LLM JSON output.");
            }
            try {
                return objectMapper.readTree(extracted);
            } catch (Exception ex) {
                throw new InternalServerException("Failed to parse extracted LLM JSON output.");
            }
        }
    }

    private String extractJsonObject(String text) {
        int start = -1;
        int level = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '{') {
                if (start < 0) {
                    start = i;
                }
                level++;
            } else if (c == '}') {
                if (level > 0) {
                    level--;
                    if (level == 0 && start >= 0) {
                        return text.substring(start, i + 1);
                    }
                }
            }
        }
        return null;
    }
}

