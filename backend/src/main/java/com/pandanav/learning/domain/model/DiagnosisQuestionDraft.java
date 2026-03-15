package com.pandanav.learning.domain.model;

import com.pandanav.learning.domain.enums.DiagnosisDimension;

/**
 * Selected question with reason and priority for adapters and rationale builder.
 */
public record DiagnosisQuestionDraft(
    DiagnosisQuestion question,
    String selectionReason,
    int dimensionPriority,
    DiagnosisDimension dimension
) {
}
