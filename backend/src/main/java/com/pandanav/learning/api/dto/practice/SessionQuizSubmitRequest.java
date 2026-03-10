package com.pandanav.learning.api.dto.practice;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record SessionQuizSubmitRequest(
    @Valid
    @NotEmpty
    List<SessionQuizAnswerRequest> answers
) {
}
