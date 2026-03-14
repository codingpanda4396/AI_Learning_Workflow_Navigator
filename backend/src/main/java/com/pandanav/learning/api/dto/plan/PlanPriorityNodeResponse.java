package com.pandanav.learning.api.dto.plan;

public record PlanPriorityNodeResponse(
    String nodeId,
    String title,
    String reason
) {
}
