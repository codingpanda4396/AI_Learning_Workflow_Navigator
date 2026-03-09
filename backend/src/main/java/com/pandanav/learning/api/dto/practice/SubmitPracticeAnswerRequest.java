package com.pandanav.learning.api.dto.practice;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record SubmitPracticeAnswerRequest(
    @NotBlank
    @JsonProperty("user_answer")
    String userAnswer
) {
}
