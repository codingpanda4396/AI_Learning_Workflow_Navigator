package com.pandanav.learning.application.service.practice;

public record PracticeGeneratorRequest(
    Long sessionId,
    Long taskId,
    Long userId,
    Long nodeId,
    String nodeTitle,
    String taskObjective,
    String stageContentJson
) {
}
