package com.pandanav.learning.domain.llm.model;

public record TutorPromptContext(
    String taskStage,
    String taskObjective,
    String nodeName,
    String sessionGoal,
    TutorReplyMode hintMode,
    TutorReplyMode directAnswerMode
) {
}
