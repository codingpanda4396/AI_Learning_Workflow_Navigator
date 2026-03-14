package com.pandanav.learning.api.dto.plan;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.OffsetDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record LearningPlanPreviewResponse(
    String planId,
    String status,
    Boolean previewOnly,
    Boolean committed,
    String goal,
    RecommendedEntryResponse recommendedEntry,
    LearnerSnapshotResponse learnerSnapshot,
    RecommendedStrategyResponse recommendedStrategy,
    List<AlternativeStrategyResponse> alternatives,
    List<String> nextActions,
    String whyThisStep,
    List<String> keyEvidence,
    String skipRisk,
    String expectedGain,
    String confidenceHint,
    LearningPlanAdjustmentsDto adjustments,
    String startGuide,
    Boolean explanationGenerated,
    OffsetDateTime generatedAt,
    String traceId
) {
    public record RecommendedEntryResponse(
        String conceptId,
        String title,
        Integer estimatedMinutes,
        String reason
    ) {
    }

    public record LearnerSnapshotResponse(
        String currentState,
        List<String> evidence
    ) {
    }

    public record RecommendedStrategyResponse(
        String code,
        String label,
        String explanation
    ) {
    }

    public record AlternativeStrategyResponse(
        String code,
        String label,
        String notRecommendedReason
    ) {
    }
}
