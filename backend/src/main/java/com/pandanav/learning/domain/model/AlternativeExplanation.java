package com.pandanav.learning.domain.model;

public record AlternativeExplanation(
    String strategyCode,
    String label,
    String reason,
    String tradeoff
) {
}
