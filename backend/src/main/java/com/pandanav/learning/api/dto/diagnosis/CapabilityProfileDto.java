package com.pandanav.learning.api.dto.diagnosis;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record CapabilityProfileDto(
    @JsonProperty("currentLevel")
    String currentLevel,
    @JsonProperty("strengths")
    List<String> strengths,
    @JsonProperty("weaknesses")
    List<String> weaknesses,
    @JsonProperty("learningPreference")
    String learningPreference,
    @JsonProperty("timeBudget")
    String timeBudget,
    @JsonProperty("goalOrientation")
    String goalOrientation,
    @JsonProperty("summary")
    String summary
) {
}
