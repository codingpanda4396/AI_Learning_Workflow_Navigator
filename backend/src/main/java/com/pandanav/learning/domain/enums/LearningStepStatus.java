package com.pandanav.learning.domain.enums;

public enum LearningStepStatus {
    TODO,
    ACTIVE,
    DONE,
    FAILED,
    SKIPPED;

    public boolean isTerminal() {
        return this == DONE || this == FAILED || this == SKIPPED;
    }
}

