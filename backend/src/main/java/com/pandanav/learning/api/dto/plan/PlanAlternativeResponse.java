package com.pandanav.learning.api.dto.plan;

public record PlanAlternativeResponse(
    String strategy,
    String label,
    String description,
    String tradeoff
) {
}
