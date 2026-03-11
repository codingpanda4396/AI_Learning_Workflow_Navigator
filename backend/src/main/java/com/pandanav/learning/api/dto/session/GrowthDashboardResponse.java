package com.pandanav.learning.api.dto.session;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pandanav.learning.api.dto.feedback.NextStepRecommendationResponse;

import java.math.BigDecimal;
import java.util.List;

public record GrowthDashboardResponse(
    @JsonProperty("session_id")
    Long sessionId,
    @JsonProperty("course_id")
    String courseId,
    @JsonProperty("chapter_id")
    String chapterId,
    @JsonProperty("learned_node_count")
    int learnedNodeCount,
    @JsonProperty("mastered_node_count")
    int masteredNodeCount,
    @JsonProperty("average_mastery_score")
    BigDecimal averageMasteryScore,
    @JsonProperty("current_node_id")
    Long currentNodeId,
    @JsonProperty("current_node_name")
    String currentNodeName,
    @JsonProperty("current_stage_code")
    String currentStageCode,
    @JsonProperty("current_stage_label")
    String currentStageLabel,
    @JsonProperty("top_weak_points")
    List<String> topWeakPoints,
    @JsonProperty("recent_performance")
    GrowthDashboardRecentPerformanceResponse recentPerformance,
    @JsonProperty("recommended_next_step")
    NextStepRecommendationResponse recommendedNextStep,
    @JsonProperty("mastery_nodes")
    List<GrowthDashboardNodeResponse> masteryNodes
) {
}
