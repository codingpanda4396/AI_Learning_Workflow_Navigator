package com.pandanav.learning.api.dto.plan;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record LearningPlanStrategyComparisonResponse(
    String currentRecommendedStrategy,
    List<LearningPlanStrategyOptionResponse> options
) {
}
