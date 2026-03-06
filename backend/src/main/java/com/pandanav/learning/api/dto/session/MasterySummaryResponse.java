package com.pandanav.learning.api.dto.session;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(name = "MasterySummaryResponse")
public record MasterySummaryResponse(
    @JsonProperty("node_id")
    @Schema(name = "node_id", example = "101")
    Long nodeId,
    @JsonProperty("node_name")
    @Schema(name = "node_name", example = "三次握手")
    String nodeName,
    @JsonProperty("mastery_value")
    @Schema(name = "mastery_value", example = "0.55")
    BigDecimal masteryValue
) {
}
