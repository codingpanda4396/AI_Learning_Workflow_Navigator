package com.pandanav.learning.domain.model;

import java.util.List;

/**
 * Rule-derived diagnosis strategy: which dimensions to prioritise,
 * target question count, tone, and reasons for explanation.
 */
public record DiagnosisStrategyDecision(
    String strategyCode,
    List<String> priorityDimensions,
    List<String> suppressedDimensions,
    int targetQuestionCount,
    String toneStyle,
    List<String> personalizationReasons
) {
    public DiagnosisStrategyDecision {
        strategyCode = strategyCode == null ? "FOUNDATION_FIRST" : strategyCode;
        priorityDimensions = priorityDimensions == null ? List.of() : List.copyOf(priorityDimensions);
        suppressedDimensions = suppressedDimensions == null ? List.of() : List.copyOf(suppressedDimensions);
        toneStyle = toneStyle == null ? "GUIDING" : toneStyle;
        personalizationReasons = personalizationReasons == null ? List.of() : List.copyOf(personalizationReasons);
    }
}
