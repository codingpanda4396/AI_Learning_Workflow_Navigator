package com.pandanav.learning.api.dto.practice;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record PracticeFeedbackReportResponse(
    @JsonProperty("report_id")
    Long reportId,
    @JsonProperty("quiz_id")
    Long quizId,
    @JsonProperty("session_id")
    Long sessionId,
    @JsonProperty("task_id")
    Long taskId,
    @JsonProperty("diagnosis_summary")
    String diagnosisSummary,
    List<String> strengths,
    List<String> weaknesses,
    @JsonProperty("review_focus")
    List<String> reviewFocus,
    @JsonProperty("next_round_advice")
    String nextRoundAdvice,
    @JsonProperty("recommended_action")
    String recommendedAction,
    String source
) {
}
