package com.pandanav.learning.api.dto.plan;

import com.pandanav.learning.api.dto.CodeLabelDto;

public record LearningPlanSummaryResponse(
    String headline,
    PlanNodeReferenceResponse recommendedStartNode,
    CodeLabelDto recommendedPace,
    Integer estimatedTotalMinutes,
    Integer estimatedNodeCount,
    Integer estimatedStageCount
) {
}
