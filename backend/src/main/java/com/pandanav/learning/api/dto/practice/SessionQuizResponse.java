package com.pandanav.learning.api.dto.practice;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record SessionQuizResponse(
    @JsonProperty("session_id")
    Long sessionId,
    @JsonProperty("task_id")
    Long taskId,
    @JsonProperty("quiz_id")
    Long quizId,
    @JsonProperty("generation_status")
    String generationStatus,
    @JsonProperty("quiz_status")
    String quizStatus,
    @JsonProperty("question_count")
    Integer questionCount,
    @JsonProperty("answered_count")
    Integer answeredCount,
    @JsonProperty("failure_reason")
    String failureReason,
    List<SessionQuizQuestionResponse> questions
) {
}
