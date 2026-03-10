package com.pandanav.learning.domain.enums;

public enum PlanMode {
    RULE,
    LLM,
    AUTO;

    public static PlanMode fromQuery(String raw) {
        if (raw == null || raw.isBlank()) {
            return AUTO;
        }
        return switch (raw.trim().toLowerCase()) {
            case "rule" -> RULE;
            case "llm" -> LLM;
            case "auto" -> AUTO;
            default -> throw new IllegalArgumentException("Unsupported plan mode: " + raw);
        };
    }
}
