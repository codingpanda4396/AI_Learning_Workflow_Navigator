package com.pandanav.learning.api.dto.plan;

import com.pandanav.learning.api.dto.CodeLabelDto;

public record PlanPathNodeResponse(
    PlanNodeReferenceResponse node,
    CodeLabelDto difficulty,
    Integer mastery,
    CodeLabelDto status,
    Boolean isRecommendedStart,
    Integer estimatedNodeMinutes,
    String reasonTag
) {
}
