package com.pandanav.learning.api.dto.practice;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record SessionQuestionResultResponse(
    @JsonProperty("question_id")
    Long questionId,
    String type,
    String stem,
    @JsonProperty("user_answer")
    String userAnswer,
    Integer score,
    boolean correct,
    String feedback,
    @JsonProperty("error_tags")
    List<String> errorTags
) {
}
