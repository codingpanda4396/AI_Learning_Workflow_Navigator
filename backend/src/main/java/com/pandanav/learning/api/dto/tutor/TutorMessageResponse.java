package com.pandanav.learning.api.dto.tutor;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

public record TutorMessageResponse(
    Long id,
    @JsonProperty("session_id")
    Long sessionId,
    @JsonProperty("task_id")
    Long taskId,
    String role,
    String content,
    @JsonProperty("created_at")
    OffsetDateTime createdAt
) {
}
