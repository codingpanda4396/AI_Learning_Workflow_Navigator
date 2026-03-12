package com.pandanav.learning.domain.model;

import java.util.List;

public record LearningPlanPreview(
    LearningPlanSummary summary,
    List<PlanReason> reasons,
    List<String> focuses,
    List<PlanPathNode> pathPreview,
    List<PlanTaskPreview> taskPreview,
    PlanAdjustments adjustments
) {
}
