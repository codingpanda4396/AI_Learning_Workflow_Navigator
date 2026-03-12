package com.pandanav.learning.domain.enums;

import java.util.EnumSet;
import java.util.Set;

public enum SessionStatus {
    ANALYZING,
    PLANNING,
    LEARNING,
    PRACTICING,
    REPORT_READY,
    COMPLETED,
    FAILED;

    private static final Set<SessionStatus> ACTIVE_STATUSES = EnumSet.of(
        ANALYZING,
        PLANNING,
        LEARNING,
        PRACTICING,
        REPORT_READY
    );

    public static SessionStatus fromDb(String value) {
        if (value == null || value.isBlank()) {
            return ANALYZING;
        }
        return switch (value.trim().toUpperCase()) {
            case "ACTIVE" -> LEARNING;
            case "GENERATING", "QUIZ_READY", "ANSWERED" -> PRACTICING;
            case "FEEDBACK_READY" -> REPORT_READY;
            case "REVIEWING" -> LEARNING;
            case "NEXT_ROUND" -> PRACTICING;
            default -> SessionStatus.valueOf(value.trim().toUpperCase());
        };
    }

    public static SessionStatus forCurrentStage(Stage stage) {
        if (stage == null) {
            return LEARNING;
        }
        return switch (stage) {
            case STRUCTURE, UNDERSTANDING, REFLECTION -> LEARNING;
            case TRAINING -> PRACTICING;
        };
    }

    public boolean isActive() {
        return ACTIVE_STATUSES.contains(this);
    }
}
