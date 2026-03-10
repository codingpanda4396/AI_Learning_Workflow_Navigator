package com.pandanav.learning.domain.llm.model;

public record GoalDiagnosisContext(
    String courseId,
    String chapterId,
    String goalText
) {
}
