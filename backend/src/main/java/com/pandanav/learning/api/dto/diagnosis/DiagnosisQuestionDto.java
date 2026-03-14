package com.pandanav.learning.api.dto.diagnosis;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pandanav.learning.api.dto.CodeLabelDto;

import java.util.List;

public record DiagnosisQuestionDto(
    @JsonProperty("questionId")
    String questionId,
    @JsonProperty("dimension")
    CodeLabelDto dimension,
    @JsonProperty("type")
    CodeLabelDto type,
    @JsonProperty("title")
    String title,
    @JsonProperty("description")
    String description,
    @JsonProperty("options")
    List<CodeLabelDto> options,
    @JsonProperty("required")
    boolean required,
    @JsonProperty("copy")
    DiagnosisQuestionCopyDto copy
) {
}
