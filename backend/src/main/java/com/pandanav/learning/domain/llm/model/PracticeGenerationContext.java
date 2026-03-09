package com.pandanav.learning.domain.llm.model;

public record PracticeGenerationContext(
    Long taskId,
    Long sessionId,
    Long nodeId,
    String nodeTitle,
    String taskObjective,
    String stageContentJson
) {
}
