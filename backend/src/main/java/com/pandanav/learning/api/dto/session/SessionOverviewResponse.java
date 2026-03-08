package com.pandanav.learning.api.dto.session;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "SessionOverviewResponse")
public record SessionOverviewResponse(
    @JsonProperty("session_id")
    @Schema(name = "session_id", example = "123")
    Long sessionId,
    @JsonProperty("course_id")
    @Schema(name = "course_id", example = "computer_network")
    String courseId,
    @JsonProperty("chapter_id")
    @Schema(name = "chapter_id", example = "tcp")
    String chapterId,
    @JsonProperty("goal_text")
    @Schema(name = "goal_text", example = "理解 TCP 可靠传输机制并能做题")
    String goalText,
    @JsonProperty("current_node_id")
    @Schema(name = "current_node_id", example = "101")
    Long currentNodeId,
    @JsonProperty("current_stage")
    @Schema(name = "current_stage", example = "UNDERSTANDING")
    String currentStage,
    List<TimelineItemResponse> timeline,
    @JsonProperty("next_task")
    NextTaskResponse nextTask,
    @JsonProperty("mastery_summary")
    List<MasterySummaryResponse> masterySummary,
    ProgressResponse progress
) {
}
