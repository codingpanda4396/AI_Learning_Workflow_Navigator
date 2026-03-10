package com.pandanav.learning.api.dto.practice;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.OffsetDateTime;
import java.util.List;

public record PracticeSubmissionResponse(
    @JsonProperty("submission_id")
    Long submissionId,
    @JsonProperty("practice_item_id")
    Long practiceItemId,
    @JsonProperty("user_answer")
    String userAnswer,
    Integer score,
    @JsonProperty("is_correct")
    Boolean isCorrect,
    String feedback,
    @JsonProperty("error_tags")
    List<String> errorTags,
    @JsonProperty("submitted_at")
    OffsetDateTime submittedAt
) {
}
