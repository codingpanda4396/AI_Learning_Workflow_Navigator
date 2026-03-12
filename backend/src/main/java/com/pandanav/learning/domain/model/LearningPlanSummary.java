package com.pandanav.learning.domain.model;

public record LearningPlanSummary(
    String headline,
    String recommendedStartNodeId,
    String recommendedStartNodeName,
    String recommendedPace,
    Integer estimatedMinutes,
    Integer estimatedNodeCount,
    Integer estimatedStageCount
) {
}
