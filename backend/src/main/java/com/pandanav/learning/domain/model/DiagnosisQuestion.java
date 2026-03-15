package com.pandanav.learning.domain.model;

import com.pandanav.learning.domain.enums.DiagnosisDimension;

import java.util.List;
import java.util.Map;

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
    String sectionLabel,
    List<String> signalTargets,
    Map<String, List<DiagnosisSignal>> optionSignalMapping
) {
    public DiagnosisQuestion(
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
        this(
            questionId,
            dimension,
            type,
            required,
            options,
            title,
            description,
            placeholder,
            submitHint,
            sectionLabel,
            List.of(),
            Map.of()
        );
    }
}
