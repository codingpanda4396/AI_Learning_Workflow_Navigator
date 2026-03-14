package com.pandanav.learning.api.dto.diagnosis;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.pandanav.learning.api.dto.CodeLabelDto;

import java.util.List;

public record CapabilityProfileDto(
    @JsonProperty("currentLevel")
    CodeLabelDto currentLevel,
    @JsonProperty("strengths")
    List<String> strengths,
    @JsonProperty("weaknesses")
    List<String> weaknesses,
    @JsonProperty("learningPreference")
    CodeLabelDto learningPreference,
    @JsonProperty("timeBudget")
    CodeLabelDto timeBudget,
    @JsonProperty("goalOrientation")
    CodeLabelDto goalOrientation,
    @JsonProperty("summary")
    String summary,
    @JsonProperty("planExplanation")
    String planExplanation
) {
}
