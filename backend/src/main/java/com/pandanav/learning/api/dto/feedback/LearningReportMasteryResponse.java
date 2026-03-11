package com.pandanav.learning.api.dto.feedback;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public record LearningReportMasteryResponse(
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
    @JsonProperty("mastery_status")
    String masteryStatus
) {
}
