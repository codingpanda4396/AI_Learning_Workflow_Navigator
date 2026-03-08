package com.pandanav.learning.api.dto.tutor;

import jakarta.validation.constraints.NotBlank;

public record TutorSendMessageRequest(
    @NotBlank
    String content
) {
}
