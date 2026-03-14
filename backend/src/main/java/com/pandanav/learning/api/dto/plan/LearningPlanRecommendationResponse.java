package com.pandanav.learning.api.dto.plan;

public record LearningPlanRecommendationResponse(
    String headline,
    String subtitle,
    String taskTitle,
    Integer estimatedMinutes,
    String priority,
    String whyNow
) {
}
