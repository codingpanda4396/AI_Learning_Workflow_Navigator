package com.pandanav.learning.api.dto.session;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public record GrowthDashboardNodeResponse(
    @JsonProperty("node_id")
    Long nodeId,
    @JsonProperty("node_name")
    String nodeName,
    @JsonProperty("mastery_score")
    BigDecimal masteryScore,
    @JsonProperty("mastery_status")
    String masteryStatus,
    @JsonProperty("is_current")
    boolean current,
    @JsonProperty("is_recommended")
    boolean recommended
) {
}
