package com.pandanav.learning.domain.llm.model;

import com.pandanav.learning.domain.model.PlanReason;
import com.pandanav.learning.domain.model.PlanAlternative;
import com.pandanav.learning.domain.model.PlanTaskPreview;
import com.pandanav.learning.domain.model.PlanGuidance;
import com.pandanav.learning.domain.model.StrategyComparison;

import java.util.List;

public record LearningPlanLlmResult(
    String headline,
    String subtitle,
    String whyNow,
    String confidence,
    String currentFocusLabel,
    String taskTitle,
    Integer taskEstimatedMinutes,
    String taskPriority,
    List<PlanReason> reasons,
    List<String> focuses,
    List<PlanAlternative> alternatives,
    StrategyComparison strategyComparison,
    List<String> benefits,
    List<String> nextUnlocks,
    String nextStepLabel,
    List<PlanTaskPreview> taskPreview,
    PlanGuidance planGuidance
) {
}
