package com.pandanav.learning.domain.service;

import com.pandanav.learning.domain.enums.Stage;

import java.util.Locale;

public final class LearningPlanStageMapper {

    private LearningPlanStageMapper() {
    }

    public static Stage normalize(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("stage is blank");
        }
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if ("EVALUATION".equals(normalized)) {
            return Stage.REFLECTION;
        }
        return Stage.valueOf(normalized);
    }
}
