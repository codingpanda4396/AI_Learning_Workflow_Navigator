package com.pandanav.learning.api.dto.plan;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record LearningPlanGuidanceResponse(
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
