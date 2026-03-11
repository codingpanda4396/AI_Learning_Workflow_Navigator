package com.pandanav.learning.api.dto.session;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

public record GrowthDashboardRecentPerformanceResponse(
    @JsonProperty("attempt_count")
    int attemptCount,
    @JsonProperty("average_score")
    BigDecimal averageScore,
    @JsonProperty("latest_score")
    Integer latestScore,
    @JsonProperty("top_error_tags")
    List<String> topErrorTags
) {
}
