package com.pandanav.learning.api.dto.task;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

import java.util.List;

@Schema(name = "AdvanceStepRequest")
public record AdvanceStepRequest(
    @NotBlank
    @Schema(example = "DONE")
    String action,
    @JsonProperty("attempt_count")
    @Positive
    @Schema(name = "attempt_count", example = "1")
    Integer attemptCount,
    @JsonProperty("evidence_types")
    List<String> evidenceTypes
) {
}

