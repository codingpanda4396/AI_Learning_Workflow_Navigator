package com.pandanav.learning.domain.model;

import com.pandanav.learning.domain.enums.DiagnosisDimension;

import java.util.List;

/**
 * A single question in the candidate pool: content plus metadata for selection.
 */
public record DiagnosisQuestionCandidate(
    String questionId,
    DiagnosisDimension dimension,
    String intentCode,
    int priorityBaseScore,
    List<String> applicableStages,
    List<String> triggerSignals,
    List<String> suppressSignals,
    DiagnosisQuestion content
) {
    public DiagnosisQuestionCandidate {
        applicableStages = applicableStages == null ? List.of() : List.copyOf(applicableStages);
        triggerSignals = triggerSignals == null ? List.of() : List.copyOf(triggerSignals);
        suppressSignals = suppressSignals == null ? List.of() : List.copyOf(suppressSignals);
    }
}
