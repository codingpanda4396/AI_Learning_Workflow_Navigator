package com.pandanav.learning.api.dto.session;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "PlanPreviewResponse")
public record PlanPreviewResponse(
    @JsonProperty("session_id")
    @Schema(name = "session_id", example = "123")
    Long sessionId,
    @JsonProperty("plan_mode")
    @Schema(name = "plan_mode", example = "AUTO")
    String planMode,
    @JsonProperty("plan_source")
    @Schema(name = "plan_source", example = "LLM")
    String planSource,
    @JsonProperty("plan_reasoning_summary")
    @Schema(name = "plan_reasoning_summary", example = "Prioritize weak nodes and add one remedial task.")
    String planReasoningSummary,
    @JsonProperty("risk_flags")
    List<String> riskFlags,
    @JsonProperty("advanced_node_ids")
    List<Long> advancedNodeIds,
    @JsonProperty("inserted_remedial_tasks")
    List<InsertedRemedialTaskResponse> insertedRemedialTasks,
    @JsonProperty("risk_summary")
    String riskSummary
) {
    public record InsertedRemedialTaskResponse(
        @JsonProperty("node_id")
        Long nodeId,
        @JsonProperty("stage")
        String stage,
        @JsonProperty("trigger")
        String trigger
    ) {
    }
}
