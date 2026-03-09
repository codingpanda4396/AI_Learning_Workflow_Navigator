package com.pandanav.learning.domain.enums;

public enum PracticeItemStatus {
    GENERATED,
    ACTIVE,
    ANSWERED,
    ARCHIVED;

    public static PracticeItemStatus fromDb(String value) {
        if (value == null || value.isBlank()) {
            return GENERATED;
        }
        return PracticeItemStatus.valueOf(value.toUpperCase());
    }
}
