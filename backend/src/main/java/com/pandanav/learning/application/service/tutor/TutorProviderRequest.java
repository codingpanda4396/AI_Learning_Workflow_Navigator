package com.pandanav.learning.application.service.tutor;

import com.pandanav.learning.domain.model.TutorMessage;

import java.util.List;

public record TutorProviderRequest(
    Long sessionId,
    Long taskId,
    Long userId,
    String taskStage,
    String taskObjective,
    String nodeName,
    String sessionGoal,
    String userMessage,
    List<TutorMessage> history
) {
}
