package com.pandanav.learning.api.dto.practice;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record PracticeQuizResponse(
    @JsonProperty("quiz_id")
    Long quizId,
    @JsonProperty("session_id")
    Long sessionId,
    @JsonProperty("task_id")
    Long taskId,
    String status,
    @JsonProperty("question_count")
    Integer questionCount,
    @JsonProperty("answered_count")
    Integer answeredCount,
    @JsonProperty("generation_source")
    String generationSource,
    @JsonProperty("failure_reason")
    String failureReason,
    List<PracticeItemResponse> questions
) {
}
