package com.pandanav.learning.api.dto.plan;

public record AdjustLearningPlanResponse(
    LearningPlanPreviewResponse result,
    String changeSummary,
    String adjustmentReason
) {
}
