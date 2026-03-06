package com.pandanav.learning.api.dto.task;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "FeedbackResponse")
public record FeedbackResponse(
    @Schema(example = "对三次握手必要性的解释不完整")
    String diagnosis,
    List<String> fixes
) {
}
