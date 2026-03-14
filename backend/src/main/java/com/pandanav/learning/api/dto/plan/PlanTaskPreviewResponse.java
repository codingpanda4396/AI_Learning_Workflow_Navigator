package com.pandanav.learning.api.dto.plan;

import com.pandanav.learning.api.dto.CodeLabelDto;

public record PlanTaskPreviewResponse(
    CodeLabelDto stage,
    String title,
    String learningGoal,
    String learnerAction,
    String aiSupport,
    Integer estimatedTaskMinutes
) {
}
