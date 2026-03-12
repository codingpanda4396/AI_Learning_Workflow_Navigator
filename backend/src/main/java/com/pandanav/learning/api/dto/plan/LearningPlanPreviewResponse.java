package com.pandanav.learning.api.dto.plan;

import java.util.List;

public record LearningPlanPreviewResponse(
    String planId,
    LearningPlanSummaryResponse summary,
    List<PlanReasonResponse> reasons,
    List<String> focuses,
    List<PlanPathNodeResponse> pathPreview,
    List<PlanTaskPreviewResponse> taskPreview,
    LearningPlanAdjustmentsRequest adjustments
) {
}
