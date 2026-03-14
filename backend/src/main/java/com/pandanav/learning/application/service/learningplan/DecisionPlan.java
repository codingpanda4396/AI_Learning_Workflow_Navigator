package com.pandanav.learning.application.service.learningplan;

import com.pandanav.learning.domain.model.PlanAlternative;
import com.pandanav.learning.domain.model.PlanReason;

import java.util.List;

public record DecisionPlan(
    String recommendedStartNodeId,
    String recommendedPace,
    List<PlanAlternative> alternatives,
    String riskAssessment,
    List<PlanReason> reasons
) {
}
