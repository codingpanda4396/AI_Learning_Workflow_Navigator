package com.pandanav.learning.domain.enums;

public enum PracticeItemSource {
    RULE,
    LLM,
    MANUAL;

    public static PracticeItemSource fromDb(String value) {
        if (value == null || value.isBlank()) {
            return RULE;
        }
        return PracticeItemSource.valueOf(value.toUpperCase());
    }
}
