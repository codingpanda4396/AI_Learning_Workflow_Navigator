package com.pandanav.learning.domain.model;

import java.util.List;
import java.util.Map;

/**
 * Structured LLM output for diagnosis strategy + question selection.
 * LLM may only choose from candidate pool; no free generation.
 */
public record DiagnosisLlmSelectionResult(
    String strategyCode,
    List<String> selectedQuestionIds,
    Map<String, Integer> questionOrder,
    Map<String, String> selectionReasons,
    List<String> suppressedQuestionIds,
    String learnerSummary
) {
    public DiagnosisLlmSelectionResult {
        selectedQuestionIds = selectedQuestionIds == null ? List.of() : List.copyOf(selectedQuestionIds);
        questionOrder = questionOrder == null ? Map.of() : Map.copyOf(questionOrder);
        selectionReasons = selectionReasons == null ? Map.of() : Map.copyOf(selectionReasons);
        suppressedQuestionIds = suppressedQuestionIds == null ? List.of() : List.copyOf(suppressedQuestionIds);
        learnerSummary = learnerSummary == null ? "" : learnerSummary.trim();
    }
}
