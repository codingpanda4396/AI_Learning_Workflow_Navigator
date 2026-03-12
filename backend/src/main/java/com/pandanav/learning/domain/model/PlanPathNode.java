package com.pandanav.learning.domain.model;

public record PlanPathNode(
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
