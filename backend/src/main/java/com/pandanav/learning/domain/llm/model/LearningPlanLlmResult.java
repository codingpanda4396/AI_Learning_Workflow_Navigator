package com.pandanav.learning.domain.llm.model;

import com.pandanav.learning.domain.model.PlanReason;
import com.pandanav.learning.domain.model.PlanTaskPreview;

import java.util.List;

public record LearningPlanLlmResult(
    String headline,
    List<PlanReason> reasons,
    List<String> focuses,
    List<PlanTaskPreview> taskPreview
) {
}
