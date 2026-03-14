package com.pandanav.learning.api.dto.plan;

import com.pandanav.learning.api.dto.CodeLabelDto;

public record LearningPlanSummaryResponse(
    String headline,
    String recommendedStartNodeId,
    String recommendedStartNodeName,
    CodeLabelDto recommendedPace,
    Integer estimatedMinutes,
    Integer estimatedNodeCount,
    Integer estimatedStageCount
) {
}
