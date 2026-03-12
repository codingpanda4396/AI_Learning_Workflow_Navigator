package com.pandanav.learning.api.dto.plan;

public record ConfirmLearningPlanResponse(
    String planId,
    Long sessionId,
    Long currentNodeId,
    Long firstTaskId,
    String nextPage
) {
}
