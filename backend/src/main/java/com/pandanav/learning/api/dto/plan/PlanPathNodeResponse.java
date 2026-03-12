package com.pandanav.learning.api.dto.plan;

public record PlanPathNodeResponse(
    String nodeId,
    String nodeName,
    Integer difficulty,
    Integer mastery,
    String status,
    Boolean isRecommendedStart,
    Integer estimatedMinutes,
    String reasonTag
) {
}
