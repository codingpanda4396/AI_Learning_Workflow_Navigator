package com.pandanav.learning.api.dto.practice;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SessionQuizAnswerRequest(
    @JsonProperty("question_id")
    @NotNull
    Long questionId,
    @JsonProperty("answer")
    @NotBlank
    String answer
) {
}
