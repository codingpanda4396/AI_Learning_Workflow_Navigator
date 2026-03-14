package com.pandanav.learning.domain.model;

import java.util.List;

public record LearningPlanPlanningContext(
    Long userId,
    String goalId,
    String diagnosisId,
    String courseId,
    String chapterId,
    String goalText,
    Long sourceSessionId,
    List<LearningPlanContextNode> nodes,
    List<String> recentErrorTags,
    List<Integer> recentScores,
    List<String> weakPointLabels,
    String learnerProfileSummary,
    PlanAdjustments adjustments,
    String requestedStrategy,
    Integer requestedTimeBudgetMinutes,
    String adjustmentReason,
    String userFeedback,
    Long basedOnPreviewId,
    LearnerStateSnapshot learnerStateSnapshot,
    PersonalizedNarrative personalizedNarrative
) {
}
