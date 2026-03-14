package com.pandanav.learning.domain.model;

import java.util.List;

public record PlanGuidance(
    String whyChosen,
    String whyNotAlternatives,
    String learnerMirror,
    String firstAction,
    String firstCheckpoint,
    String planTradeoff,
    String ifPerformWell,
    String ifStillStruggle,
    String ifNoTime,
    String startPrompt,
    List<String> kickoffSteps,
    String warmupGoal,
    String validationFocus,
    String evidenceMode,
    String adaptationPolicy,
    String confidenceExplanation
) {
}
