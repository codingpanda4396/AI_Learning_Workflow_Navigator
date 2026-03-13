package com.pandanav.learning.api.dto.plan;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record LearningPlanPreviewResponse(
    String planId,
    LearningPlanSummaryResponse summary,
    List<PlanReasonResponse> reasons,
    List<String> focuses,
    List<PlanPathNodeResponse> pathPreview,
    List<PlanTaskPreviewResponse> taskPreview,
    LearningPlanAdjustmentsRequest adjustments,
    String planSource,
    Boolean fallbackApplied,
    List<String> fallbackReasons
) {
}
