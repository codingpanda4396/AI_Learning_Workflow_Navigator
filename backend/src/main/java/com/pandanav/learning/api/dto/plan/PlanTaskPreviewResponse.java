package com.pandanav.learning.api.dto.plan;

import com.pandanav.learning.api.dto.CodeLabelDto;

public record PlanTaskPreviewResponse(
    CodeLabelDto stage,
    String title,
    String goal,
    String learnerAction,
    String aiSupport,
    Integer estimatedMinutes
) {
}
