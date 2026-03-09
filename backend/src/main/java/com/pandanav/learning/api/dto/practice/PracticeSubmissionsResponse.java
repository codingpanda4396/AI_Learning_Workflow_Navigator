package com.pandanav.learning.api.dto.practice;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record PracticeSubmissionsResponse(
    @JsonProperty("session_id")
    Long sessionId,
    @JsonProperty("task_id")
    Long taskId,
    @JsonProperty("submission_count")
    int submissionCount,
    List<PracticeSubmissionResponse> submissions
) {
}
