package com.pandanav.learning.api.dto.plan;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.pandanav.learning.api.dto.CodeLabelDto;

import java.time.OffsetDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record LearningPlanPreviewResponse(
    String previewId,
    String status,
    Boolean previewOnly,
    Boolean committed,
    CodeLabelDto planSource,
    CodeLabelDto contentSource,
    String contentSourceType,
    Boolean fallbackApplied,
    List<String> fallbackReasons,
    String confidence,
    OffsetDateTime generatedAt,
    String traceId,
    LearningPlanSummaryResponse summary,
    List<PlanReasonResponse> reasons,
    List<PlanReasonResponse> decisionReasons,
    List<PlanAlternativeResponse> alternatives,
    List<String> focuses,
    LearningPlanRecommendationResponse recommendation,
    LearningPlanLearnerSnapshotResponse learnerSnapshot,
    String whyStartHere,
    List<String> keyWeaknesses,
    List<PlanPriorityNodeResponse> priorityNodes,
    List<PlanPathNodeResponse> pathPreview,
    List<PlanTaskPreviewResponse> taskPreview,
    List<String> benefits,
    List<String> nextUnlocks,
    String nextStepLabel,
    LearningPlanAdjustmentsDto adjustments,
    LearningPlanContextResponse context,
    String nextStepNote,
    LearningPlanMetadataResponse metadata
) {
}
