package com.pandanav.learning.application.command;

public record AdjustLearningPlanCommand(
    Long userId,
    Long sessionId,
    Long previewId,
    String strategy,
    String reason,
    Integer timeBudget,
    String userFeedback
) {
}
