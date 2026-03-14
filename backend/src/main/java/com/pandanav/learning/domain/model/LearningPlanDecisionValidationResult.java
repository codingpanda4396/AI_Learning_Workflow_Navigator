package com.pandanav.learning.domain.model;

import com.pandanav.learning.domain.enums.DecisionFallbackLevel;

import java.util.List;

public record LearningPlanDecisionValidationResult(
    LlmPlanDecisionResult finalDecision,
    DecisionFallbackLevel fallbackLevel,
    List<String> fallbackReasons
) {
    public boolean fallbackApplied() {
        return fallbackLevel != DecisionFallbackLevel.NONE;
    }
}
