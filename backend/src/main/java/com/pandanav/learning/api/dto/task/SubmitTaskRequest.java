package com.pandanav.learning.api.dto.task;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(name = "SubmitTaskRequest")
public record SubmitTaskRequest(
    @NotBlank
    @JsonProperty("user_answer")
    @Schema(name = "user_answer", example = "I think two-way handshake is enough...")
    String userAnswer
) {
}
