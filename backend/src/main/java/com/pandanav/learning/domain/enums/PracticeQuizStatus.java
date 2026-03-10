package com.pandanav.learning.domain.enums;

public enum PracticeQuizStatus {
    GENERATING,
    QUIZ_READY,
    ANSWERED,
    FEEDBACK_READY,
    REVIEWING,
    NEXT_ROUND,
    FAILED;

    public static PracticeQuizStatus fromDb(String value) {
        if (value == null || value.isBlank()) {
            return GENERATING;
        }
        return PracticeQuizStatus.valueOf(value.trim().toUpperCase());
    }
}
