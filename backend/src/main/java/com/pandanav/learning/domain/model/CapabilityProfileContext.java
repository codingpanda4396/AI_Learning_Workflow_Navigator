package com.pandanav.learning.domain.model;

public record CapabilityProfileContext(
    Long sessionId,
    String currentLevel,
    String goalOrientation,
    String timeBudget,
    String learningPreference,
    String summary
) {
}
