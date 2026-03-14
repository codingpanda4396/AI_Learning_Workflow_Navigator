package com.pandanav.learning.domain.model;

public record StrategyOptionComparison(
    String strategy,
    String label,
    String suitableFor,
    String notIdealWhen,
    String switchingCostRisk
) {
}
