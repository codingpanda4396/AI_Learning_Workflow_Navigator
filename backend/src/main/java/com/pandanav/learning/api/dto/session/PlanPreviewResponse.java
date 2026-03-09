package com.pandanav.learning.api.dto.session;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "PlanPreviewResponse")
public record PlanPreviewResponse(
    @JsonProperty("session_id")
    @Schema(name = "session_id", example = "123")
    Long sessionId,
    @JsonProperty("plan_source")
    @Schema(name = "plan_source", example = "LLM")
    String planSource,
    @JsonProperty("plan_reasoning_summary")
    @Schema(name = "plan_reasoning_summary", example = "Prioritize weak nodes and add one remedial task.")
    String planReasoningSummary,
    @JsonProperty("risk_flags")
    List<String> riskFlags
) {
}
