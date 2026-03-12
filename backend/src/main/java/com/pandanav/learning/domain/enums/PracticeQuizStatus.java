package com.pandanav.learning.domain.enums;

public enum PracticeQuizStatus {
    GENERATING,
    READY,
    ANSWERING,
    REVIEWING,
    REPORT_READY,
    NEXT_ROUND,
    FAILED;

    public static PracticeQuizStatus fromDb(String value) {
        if (value == null || value.isBlank()) {
            return GENERATING;
        }
        return switch (value.trim().toUpperCase()) {
            case "QUIZ_READY" -> READY;
            case "ANSWERED" -> ANSWERING;
            case "FEEDBACK_READY" -> REPORT_READY;
            default -> PracticeQuizStatus.valueOf(value.trim().toUpperCase());
        };
    }
}
