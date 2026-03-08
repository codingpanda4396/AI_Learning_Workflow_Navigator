package com.pandanav.learning.api.dto.tutor;

import com.fasterxml.jackson.annotation.JsonProperty;

public record TutorSendMessageResponse(
    @JsonProperty("session_id")
    Long sessionId,
    @JsonProperty("task_id")
    Long taskId,
    @JsonProperty("user_message")
    TutorMessageResponse userMessage,
    @JsonProperty("assistant_message")
    TutorMessageResponse assistantMessage
) {
}
