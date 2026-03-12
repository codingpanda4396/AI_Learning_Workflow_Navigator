package com.pandanav.learning.api.dto.plan;

public record PlanTaskPreviewResponse(
    String stage,
    String title,
    String goal,
    String learnerAction,
    String aiSupport,
    Integer estimatedMinutes
) {
}
