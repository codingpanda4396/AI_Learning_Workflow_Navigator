package com.pandanav.learning.api.dto.task;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "TaskDetailResponse")
public record TaskDetailResponse(
    @JsonProperty("task_id")
    @Schema(name = "task_id", example = "1002")
    Long taskId,
    @JsonProperty("session_id")
    @Schema(name = "session_id", example = "2001")
    Long sessionId,
    @JsonProperty("node_id")
    @Schema(name = "node_id", example = "101")
    Long nodeId,
    @JsonProperty("node_name")
    @Schema(name = "node_name", example = "Binary Search")
    String nodeName,
    @Schema(example = "UNDERSTANDING")
    String stage,
    @Schema(example = "Explain core mechanism and boundary conditions.")
    String objective,
    @Schema(example = "SUCCEEDED")
    String status,
    @JsonProperty("has_output")
    @Schema(name = "has_output", example = "true")
    boolean hasOutput,
    JsonNode output
) {
}
