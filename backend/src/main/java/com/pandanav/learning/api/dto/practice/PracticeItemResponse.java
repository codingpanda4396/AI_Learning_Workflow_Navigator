package com.pandanav.learning.api.dto.practice;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

public record PracticeItemResponse(
    @JsonProperty("item_id")
    Long itemId,
    @JsonProperty("question_type")
    String questionType,
    String stem,
    JsonNode options,
    String difficulty,
    String source,
    String status
) {
}
