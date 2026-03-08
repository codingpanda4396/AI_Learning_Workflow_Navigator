package com.pandanav.learning.api.dto.task;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "RunTaskResponse")
public record RunTaskResponse(
    @JsonProperty("task_id")
    @Schema(name = "task_id", example = "1002")
    Long taskId,
    @Schema(example = "UNDERSTANDING")
    String stage,
    @JsonProperty("node_id")
    @Schema(name = "node_id", example = "101")
    Long nodeId,
    @Schema(example = "SUCCEEDED")
    String status,
    @JsonProperty("generation_mode")
    @Schema(name = "generation_mode", example = "LLM")
    String generationMode,
    JsonNode output
) {
}
