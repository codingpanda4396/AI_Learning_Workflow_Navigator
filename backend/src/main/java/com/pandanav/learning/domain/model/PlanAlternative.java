package com.pandanav.learning.domain.model;

public record PlanAlternative(
    String strategy,
    String label,
    String description,
    String tradeoff
) {
}
