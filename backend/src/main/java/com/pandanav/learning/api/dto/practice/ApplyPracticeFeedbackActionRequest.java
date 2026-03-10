package com.pandanav.learning.api.dto.practice;

import jakarta.validation.constraints.NotBlank;

public record ApplyPracticeFeedbackActionRequest(
    @NotBlank
    String action
) {
}
