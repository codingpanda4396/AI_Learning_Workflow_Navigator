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
    List<String> profileDrivenReasoning,
    String skipRisk,
    String expectedGain,
    String confidenceHint,
    List<String> riskFlags,
    List<String> profileConflicts,
    LearningPlanAdjustmentsDto adjustments,
    String startGuide,
    Boolean explanationGenerated,
    PersonalizedSummaryResponse personalizedSummary,
    CurrentTaskCardResponse currentTaskCard,
    PersonalizedReasonsResponse personalizedReasons,
    ExplanationPanelResponse explanationPanel,
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

    public record PersonalizedSummaryResponse(
        String title,
        String description,
        List<String> tags
    ) {
    }

    public record CurrentTaskCardResponse(
        String title,
        Integer estimatedMinutes,
        String goal,
        List<String> tasks,
        List<String> completionGains
    ) {
    }

    public record PersonalizedReasonsResponse(
        List<String> whyRecommended,
        List<String> whyThisStepFirst
    ) {
    }

    public record ExplanationPanelResponse(
        List<LearnerProfileItemResponse> learnerProfile,
        String systemDecision
    ) {
    }

    public record LearnerProfileItemResponse(
        String label,
        String value
    ) {
    }
}
