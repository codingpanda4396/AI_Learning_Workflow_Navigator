package com.pandanav.learning.api.dto.task;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "FeedbackResponse")
public record FeedbackResponse(
    @Schema(example = "Explanation misses key causal steps.")
    String diagnosis,
    List<String> fixes
) {
}
