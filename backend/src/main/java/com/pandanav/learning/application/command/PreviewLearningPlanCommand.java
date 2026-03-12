package com.pandanav.learning.application.command;

import com.pandanav.learning.api.dto.plan.LearningPlanAdjustmentsRequest;

public record PreviewLearningPlanCommand(
    Long userId,
    String goalId,
    String diagnosisId,
    String courseId,
    String chapterId,
    String goalText,
    LearningPlanAdjustmentsRequest adjustments
) {
}
