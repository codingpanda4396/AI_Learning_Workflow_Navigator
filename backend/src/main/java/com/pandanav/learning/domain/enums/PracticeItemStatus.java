package com.pandanav.learning.domain.enums;

public enum PracticeItemStatus {
    READY,
    ANSWERED,
    ARCHIVED;

    public static PracticeItemStatus fromDb(String value) {
        if (value == null || value.isBlank()) {
            return READY;
        }
        return switch (value.trim().toUpperCase()) {
            case "GENERATED", "ACTIVE" -> READY;
            default -> PracticeItemStatus.valueOf(value.trim().toUpperCase());
        };
    }
}
