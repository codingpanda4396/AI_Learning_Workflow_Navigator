package com.pandanav.learning.api.dto.task;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.pandanav.learning.api.dto.session.NextTaskResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;

@Schema(name = "SubmitTaskResponse")
public record SubmitTaskResponse(
    @JsonProperty("task_id")
    @Schema(name = "task_id", example = "1003")
    Long taskId,
    @Schema(example = "TRAINING")
    String stage,
    @JsonProperty("node_id")
    @Schema(name = "node_id", example = "101")
    Long nodeId,
    @Schema(example = "72")
    Integer score,
    @JsonProperty("normalized_score")
    @Schema(example = "0.720")
    BigDecimal normalizedScore,
    @JsonProperty("error_tags")
    List<String> errorTags,
    FeedbackResponse feedback,
    JsonNode rubric,
    List<String> strengths,
    List<String> weaknesses,
    @JsonProperty("mastery_before")
    BigDecimal masteryBefore,
    @JsonProperty("mastery_delta")
    BigDecimal masteryDelta,
    @JsonProperty("mastery_after")
    BigDecimal masteryAfter,
    @JsonProperty("next_action")
    String nextAction,
    @JsonProperty("next_task")
    NextTaskResponse nextTask
) {
}
