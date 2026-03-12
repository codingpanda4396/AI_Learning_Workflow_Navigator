package com.pandanav.learning.domain.model;

public record LearningPlanAggregate(
    LearningPlan plan,
    LearningPlanPreview preview,
    LearningPlanPlanningContext planningContext
) {
}
