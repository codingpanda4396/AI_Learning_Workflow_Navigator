package com.pandanav.learning.domain.enums;

public enum TutorMessageRole {
    USER,
    ASSISTANT;

    public String toApiRole() {
        return name().toLowerCase();
    }
}
