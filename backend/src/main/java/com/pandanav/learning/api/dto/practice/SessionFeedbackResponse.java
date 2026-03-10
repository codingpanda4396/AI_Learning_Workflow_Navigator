package com.pandanav.learning.api.dto.practice;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record SessionFeedbackResponse(
    @JsonProperty("report_id")
    Long reportId,
    @JsonProperty("quiz_id")
    Long quizId,
    @JsonProperty("session_id")
    Long sessionId,
    @JsonProperty("task_id")
    Long taskId,
    @JsonProperty("report_status")
    String reportStatus,
    @JsonProperty("overall_summary")
    String overallSummary,
    @JsonProperty("question_results")
    List<SessionQuestionResultResponse> questionResults,
    List<String> strengths,
    List<String> weaknesses,
    @JsonProperty("review_focus")
    List<String> reviewFocus,
    @JsonProperty("next_round_advice")
    String nextRoundAdvice,
    @JsonProperty("suggested_next_action")
    String suggestedNextAction,
    @JsonProperty("recommended_action")
    String recommendedAction,
    @JsonProperty("selected_action")
    String selectedAction,
    String source
) {
}
