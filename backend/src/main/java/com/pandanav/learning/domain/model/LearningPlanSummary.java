package com.pandanav.learning.domain.model;

import java.util.List;

public record LearningPlanSummary(
    String headline,
    String recommendedStartNodeId,
    String recommendedStartNodeName,
    String recommendedPace,
    String selectedStrategyCode,
    Integer estimatedMinutes,
    Integer estimatedNodeCount,
    Integer estimatedStageCount,
    String subtitle,
    String whyNow,
    String confidence,
    String currentFocusLabel,
    String taskTitle,
    Integer taskEstimatedMinutes,
    String taskPriority,
    List<PlanAlternative> alternatives,
    List<String> benefits,
    List<String> nextUnlocks,
    String nextStepLabel,
    String contentSourceType,
    Boolean fallbackApplied,
    List<String> fallbackReasons
) {
}
