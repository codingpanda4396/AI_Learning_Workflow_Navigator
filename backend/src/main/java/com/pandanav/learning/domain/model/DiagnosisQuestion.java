package com.pandanav.learning.domain.model;

import com.pandanav.learning.domain.enums.DiagnosisDimension;

import java.util.List;

public record DiagnosisQuestion(
    String questionId,
    DiagnosisDimension dimension,
    String type,
    boolean required,
    List<DiagnosisQuestionOption> options,
    String title,
    String description,
    String placeholder,
    String submitHint,
    String sectionLabel
) {
}
