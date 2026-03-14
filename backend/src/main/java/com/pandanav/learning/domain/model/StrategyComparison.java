package com.pandanav.learning.domain.model;

import java.util.List;

public record StrategyComparison(
    String currentRecommendedStrategy,
    List<StrategyOptionComparison> options
) {
}
