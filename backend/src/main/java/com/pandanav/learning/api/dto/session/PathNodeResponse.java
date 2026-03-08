package com.pandanav.learning.api.dto.session;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(name = "PathNodeResponse")
public record PathNodeResponse(
    @JsonProperty("node_id")
    @Schema(name = "node_id", example = "101")
    Long nodeId,
    @JsonProperty("node_name")
    @Schema(name = "node_name", example = "TCP handshake")
    String nodeName,
    @Schema(example = "IN_PROGRESS")
    String status,
    @JsonProperty("mastery_value")
    @Schema(name = "mastery_value", example = "0.55")
    BigDecimal masteryValue
) {
}
