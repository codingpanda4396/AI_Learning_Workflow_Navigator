package com.pandanav.learning.domain.model;

import com.pandanav.learning.domain.enums.CapabilityLevel;

import java.util.List;

public record CapabilityProfileDraft(
    CapabilityLevel currentLevel,
    List<String> strengths,
    List<String> weaknesses,
    String learningPreference,
    String timeBudget,
    String goalOrientation,
    String summaryText
) {
}
