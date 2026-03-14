package com.pandanav.learning.api.dto.plan;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.pandanav.learning.api.dto.CodeLabelDto;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record LearningPlanPreviewResponse(
    String previewId,
    String status,
    Boolean previewOnly,
    Boolean committed,
    CodeLabelDto planSource,
    CodeLabelDto contentSource,
    Boolean fallbackApplied,
    List<String> fallbackReasons,
    LearningPlanSummaryResponse summary,
    List<PlanReasonResponse> reasons,
    List<String> focuses,
    List<PlanPathNodeResponse> pathPreview,
    List<PlanTaskPreviewResponse> taskPreview,
    LearningPlanAdjustmentsDto adjustments,
    LearningPlanContextResponse context,
    String nextStepNote,
    LearningPlanMetadataResponse metadata
) {
}
