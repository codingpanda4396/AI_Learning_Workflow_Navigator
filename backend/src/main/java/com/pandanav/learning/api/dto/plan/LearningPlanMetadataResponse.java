package com.pandanav.learning.api.dto.plan;

public record LearningPlanMetadataResponse(
    String schemaVersion,
    boolean persistedPreview,
    String estimatedTotalMinutesScope,
    String estimatedNodeMinutesScope,
    String estimatedTaskMinutesScope
) {
}
