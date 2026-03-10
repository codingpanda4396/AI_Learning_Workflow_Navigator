package com.pandanav.learning.api.dto.feedback;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

public record WeakPointNodeResponse(
    @JsonProperty("node_id")
    Long nodeId,
    @JsonProperty("node_name")
    String nodeName,
    @JsonProperty("mastery_score")
    BigDecimal masteryScore,
    @JsonProperty("training_accuracy")
    BigDecimal trainingAccuracy,
    @JsonProperty("latest_evaluation_score")
    Integer latestEvaluationScore,
    @JsonProperty("attempt_count")
    Integer attemptCount,
    @JsonProperty("recent_error_tags")
    List<String> recentErrorTags,
    List<String> reasons
) {
}
