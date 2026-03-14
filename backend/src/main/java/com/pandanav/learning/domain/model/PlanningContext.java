package com.pandanav.learning.domain.model;

public record PlanningContext(
    String learningGoal,
    String topicName,
    String chapterName
) {
}
