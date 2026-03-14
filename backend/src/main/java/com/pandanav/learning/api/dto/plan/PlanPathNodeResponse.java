package com.pandanav.learning.api.dto.plan;

import com.pandanav.learning.api.dto.CodeLabelDto;

public record PlanPathNodeResponse(
    String nodeId,
    String nodeName,
    CodeLabelDto difficulty,
    Integer mastery,
    CodeLabelDto status,
    Boolean isRecommendedStart,
    Integer estimatedMinutes,
    String reasonTag
) {
}
