package com.pandanav.learning.domain.enums;

public enum PracticeFeedbackAction {
    REVIEW,
    NEXT_ROUND;

    public static PracticeFeedbackAction fromValue(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("action must not be blank.");
        }
        return PracticeFeedbackAction.valueOf(value.trim().toUpperCase());
    }
}
