package com.pandanav.learning.api.dto.session;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "PathResponse")
public record PathResponse(
    @JsonProperty("session_id")
    @Schema(name = "session_id", example = "123")
    Long sessionId,
    @JsonProperty("current_node_id")
    @Schema(name = "current_node_id", example = "101")
    Long currentNodeId,
    List<PathNodeResponse> nodes
) {
}
