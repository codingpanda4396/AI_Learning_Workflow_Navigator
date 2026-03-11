package com.pandanav.learning.api.dto.feedback;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

public record LearningReportResponse(
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
    List<String> strengths,
    List<String> weaknesses,
    @JsonProperty("review_focus")
    List<String> reviewFocus,
    LearningReportMasteryResponse mastery,
    @JsonProperty("question_results")
    List<LearningReportQuestionResponse> questionResults,
    @JsonProperty("weak_points")
    List<WeakPointNodeResponse> weakPoints,
    @JsonProperty("next_step")
    NextStepRecommendationResponse nextStep,
    @JsonProperty("growth_recorded")
    boolean growthRecorded
) {
}
