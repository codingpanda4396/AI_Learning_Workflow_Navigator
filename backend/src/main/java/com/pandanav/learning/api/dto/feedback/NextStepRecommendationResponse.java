package com.pandanav.learning.api.dto.feedback;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public record NextStepRecommendationResponse(
    @JsonProperty("recommended_action")
    String recommendedAction,
    String reason,
    @JsonProperty("target_node_id")
    Long targetNodeId,
    @JsonProperty("target_node_name")
    String targetNodeName,
    @JsonProperty("target_task_type")
    String targetTaskType,
    BigDecimal confidence
) {
}
