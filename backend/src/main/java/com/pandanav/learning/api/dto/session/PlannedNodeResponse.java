package com.pandanav.learning.api.dto.session;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "PlannedNodeResponse")
public record PlannedNodeResponse(
    @JsonProperty("node_id")
    @Schema(name = "node_id", example = "101")
    Long nodeId,
    @JsonProperty("node_name")
    @Schema(name = "node_name", example = "Context Switching")
    String nodeName,
    @Schema(example = "PENDING")
    String status,
    List<PlannedNodeStageResponse> stages
) {
}
