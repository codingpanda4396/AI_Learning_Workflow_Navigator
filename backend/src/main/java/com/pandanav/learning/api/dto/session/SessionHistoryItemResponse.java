package com.pandanav.learning.api.dto.session;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;

public record SessionHistoryItemResponse(
    @JsonProperty("session_id")
    Long sessionId,
    String course,
    String chapter,
    String goal,
    String status,
    ProgressResponse progress,
    @JsonProperty("last_active_at")
    OffsetDateTime lastActiveAt
) {
}
