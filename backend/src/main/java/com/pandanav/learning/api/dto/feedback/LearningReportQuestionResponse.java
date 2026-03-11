package com.pandanav.learning.api.dto.feedback;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record LearningReportQuestionResponse(
    @JsonProperty("practice_item_id")
    Long practiceItemId,
    @JsonProperty("question_type")
    String questionType,
    String stem,
    Integer score,
    Boolean correct,
    String feedback,
    @JsonProperty("error_tags")
    List<String> errorTags
) {
}
