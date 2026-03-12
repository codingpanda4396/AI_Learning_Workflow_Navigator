package com.pandanav.learning.domain.model;

import com.pandanav.learning.domain.enums.DiagnosisDimension;

import java.util.List;

public record DiagnosisQuestion(
    String questionId,
    DiagnosisDimension dimension,
    String type,
    String title,
    String description,
    List<String> options,
    boolean required,
    DiagnosisQuestionCopy copy
) {
}
