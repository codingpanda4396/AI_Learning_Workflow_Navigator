package com.pandanav.learning.api.dto.plan;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record LearningPlanPersonalizationResponse(
    String learnerState,
    List<String> whatISaw,
    String whyThisPlanFitsYou,
    String mainRiskIfSkip,
    String thisRoundBoundary,
    String adaptationHint,
    String personalizedEvidence,
    String riskPrediction
) {
}
