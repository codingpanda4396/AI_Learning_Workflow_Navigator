package com.pandanav.learning.api.dto.tutor;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record TutorMessageListResponse(
    @JsonProperty("session_id")
    Long sessionId,
    @JsonProperty("task_id")
    Long taskId,
    List<TutorMessageResponse> messages
) {
}
