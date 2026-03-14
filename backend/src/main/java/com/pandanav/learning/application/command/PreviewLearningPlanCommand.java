package com.pandanav.learning.application.command;

import com.pandanav.learning.api.dto.plan.LearningPlanAdjustmentsDto;

public record PreviewLearningPlanCommand(
    Long userId,
    String diagnosisId,
    Long sessionId,
    String courseName,
    String chapterName,
    String goalText,
    LearningPlanAdjustmentsDto adjustments
) {
}
