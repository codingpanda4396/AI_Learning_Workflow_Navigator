package com.pandanav.learning.api.dto.plan;

public record LearningPlanSummaryResponse(
    String headline,
    String recommendedStartNodeId,
    String recommendedStartNodeName,
    String recommendedPace,
    Integer estimatedMinutes,
    Integer estimatedNodeCount,
    Integer estimatedStageCount
) {
}
