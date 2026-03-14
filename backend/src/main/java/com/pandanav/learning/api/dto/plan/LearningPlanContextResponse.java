package com.pandanav.learning.api.dto.plan;

public record LearningPlanContextResponse(
    Long sessionId,
    String diagnosisId,
    String goalText,
    String courseName,
    String chapterName,
    String diagnosisSummary
) {
}
