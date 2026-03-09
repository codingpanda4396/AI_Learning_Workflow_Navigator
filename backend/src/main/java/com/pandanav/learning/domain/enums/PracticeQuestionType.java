package com.pandanav.learning.domain.enums;

public enum PracticeQuestionType {
    SINGLE_CHOICE,
    TRUE_FALSE,
    SHORT_ANSWER;

    public static PracticeQuestionType fromDb(String value) {
        if (value == null || value.isBlank()) {
            return SHORT_ANSWER;
        }
        return PracticeQuestionType.valueOf(value.toUpperCase());
    }
}
