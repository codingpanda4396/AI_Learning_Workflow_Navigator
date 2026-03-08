package com.pandanav.learning.api.dto.session;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(name = "ProgressResponse")
public record ProgressResponse(
    @JsonProperty("completed_task_count")
    @Schema(name = "completed_task_count", example = "3")
    int completedTaskCount,
    @JsonProperty("total_task_count")
    @Schema(name = "total_task_count", example = "5")
    int totalTaskCount,
    @JsonProperty("completion_rate")
    @Schema(name = "completion_rate", example = "0.60")
    BigDecimal completionRate
) {
}
