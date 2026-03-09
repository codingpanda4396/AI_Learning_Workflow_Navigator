package com.pandanav.learning.application.service.pathplan;

import com.pandanav.learning.domain.enums.PlanSource;
import com.pandanav.learning.domain.model.ConceptNode;
import com.pandanav.learning.domain.llm.model.PersonalizedPathPlan;

import java.util.List;

public record PersonalizedPlanResult(
    PlanSource source,
    List<ConceptNode> orderedNodes,
    List<PersonalizedPathPlan.InsertedTask> insertedTasks,
    String planReasoningSummary,
    List<String> riskFlags,
    String promptKey,
    String promptVersion,
    String provider,
    String model,
    boolean fallbackApplied,
    List<String> validationErrors,
    boolean shadowMode
) {
}
