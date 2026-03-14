package com.pandanav.learning.api.dto.plan;

public record LearningPlanStrategyOptionResponse(
    String strategy,
    String label,
    String suitableFor,
    String notIdealWhen,
    String switchingCostRisk
) {
}
