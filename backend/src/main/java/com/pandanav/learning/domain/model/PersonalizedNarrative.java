package com.pandanav.learning.domain.model;

import java.util.List;

public record PersonalizedNarrative(
    String learnerState,
    List<String> whatISaw,
    String whyThisPlanFitsYou,
    String mainRiskIfSkip,
    String thisRoundBoundary,
    String adaptationHint
) {
}
