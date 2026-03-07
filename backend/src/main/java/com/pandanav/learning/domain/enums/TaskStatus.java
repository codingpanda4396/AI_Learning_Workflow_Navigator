package com.pandanav.learning.domain.enums;

public enum TaskStatus {
    PENDING,
    RUNNING,
    SUCCEEDED,
    FAILED;

    public static TaskStatus fromDb(String value) {
        if (value == null || value.isBlank()) {
            return PENDING;
        }
        if ("CANCELLED".equalsIgnoreCase(value)) {
            return FAILED;
        }
        return TaskStatus.valueOf(value.toUpperCase());
    }
}


