package com.pandanav.learning.domain.model;

public record DiagnosisQuestionCopy(
    String sectionLabel,
    String title,
    String description,
    String placeholder,
    String submitHint
) {
}
