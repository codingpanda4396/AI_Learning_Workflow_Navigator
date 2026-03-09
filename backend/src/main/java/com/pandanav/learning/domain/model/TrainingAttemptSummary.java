package com.pandanav.learning.domain.model;

import java.util.List;

public record TrainingAttemptSummary(
    Long taskId,
    Long nodeId,
    Integer score,
    List<String> errorTags
) {
}
