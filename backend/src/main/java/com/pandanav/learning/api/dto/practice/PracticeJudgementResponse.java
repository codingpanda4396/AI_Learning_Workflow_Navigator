package com.pandanav.learning.api.dto.practice;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record PracticeJudgementResponse(
    Integer score,
    @JsonProperty("is_correct")
    Boolean isCorrect,
    String feedback,
    @JsonProperty("error_tags")
    List<String> errorTags
) {
}
