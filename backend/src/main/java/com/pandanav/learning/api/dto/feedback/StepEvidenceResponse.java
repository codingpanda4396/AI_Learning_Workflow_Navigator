package com.pandanav.learning.api.dto.feedback;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

public record StepEvidenceResponse(
    @JsonProperty("evidence_id")
    Long evidenceId,
    @JsonProperty("step_id")
    Long stepId,
    @JsonProperty("step_index")
    Integer stepIndex,
    @JsonProperty("evidence_type")
    String evidenceType,
    String summary,
    @JsonProperty("created_at")
    OffsetDateTime createdAt
) {
}
