package com.pandanav.learning.domain.model;

import java.util.List;

public record LlmPlanDecisionResult(
    String selectedConceptId,
    String selectedStrategyCode,
    String selectedIntensityCode,
    String heroReason,
    String currentStateSummary,
    List<String> evidenceBullets,
    List<AlternativeExplanation> alternativeExplanations,
    List<String> nextActions
) {
}
