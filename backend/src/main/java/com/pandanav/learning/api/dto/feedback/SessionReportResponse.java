package com.pandanav.learning.api.dto.feedback;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

public record SessionReportResponse(
    @JsonProperty("session_id")
    Long sessionId,
    @JsonProperty("task_id")
    Long taskId,
    @JsonProperty("node_id")
    Long nodeId,
    @JsonProperty("node_name")
    String nodeName,
    @JsonProperty("stage_code")
    String stageCode,
    @JsonProperty("stage_label")
    String stageLabel,
    @JsonProperty("overall_score")
    Integer overallScore,
    @JsonProperty("overall_accuracy")
    BigDecimal overallAccuracy,
    @JsonProperty("correct_count")
    Integer correctCount,
    @JsonProperty("question_count")
    Integer questionCount,
    @JsonProperty("diagnosis_summary")
    String diagnosisSummary,
    @JsonProperty("overall_summary")
    String overallSummary,
    List<String> strengths,
    List<String> weaknesses,
    @JsonProperty("review_focus")
    List<String> reviewFocus,
    @JsonProperty("question_results")
    List<LearningReportQuestionResponse> questionResults,
    @JsonProperty("weak_points")
    List<WeakPointNodeResponse> weakPoints,
    @JsonProperty("next_round_advice")
    String nextRoundAdvice,
    @JsonProperty("suggested_next_action")
    String suggestedNextAction,
    @JsonProperty("recommended_action")
    String recommendedAction,
    @JsonProperty("selected_action")
    String selectedAction,
    @JsonProperty("next_step")
    NextStepRecommendationResponse nextStep,
    @JsonProperty("growth_recorded")
    boolean growthRecorded,
    String source
) {
}
