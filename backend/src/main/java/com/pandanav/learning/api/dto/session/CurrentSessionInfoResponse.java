package com.pandanav.learning.api.dto.session;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "CurrentSessionInfoResponse")
public record CurrentSessionInfoResponse(
    @JsonProperty("session_id")
    @Schema(name = "session_id", example = "2001")
    Long sessionId,
    @JsonProperty("user_id")
    @Schema(name = "user_id", example = "user-123")
    String userId,
    @JsonProperty("course_id")
    @Schema(name = "course_id", example = "course-01")
    String courseId,
    @JsonProperty("chapter_id")
    @Schema(name = "chapter_id", example = "chapter-01")
    String chapterId,
    @JsonProperty("goal_text")
    @Schema(name = "goal_text", example = "Master chapter basics.")
    String goalText,
    @JsonProperty("current_node_id")
    @Schema(name = "current_node_id", example = "101")
    Long currentNodeId,
    @JsonProperty("current_stage")
    @Schema(name = "current_stage", example = "STRUCTURE")
    String currentStage,
    @JsonProperty("session_status")
    @Schema(name = "session_status", example = "LEARNING")
    String sessionStatus
) {
}
