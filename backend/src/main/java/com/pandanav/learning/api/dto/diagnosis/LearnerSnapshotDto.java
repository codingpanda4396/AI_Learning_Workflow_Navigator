package com.pandanav.learning.api.dto.diagnosis;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Front-facing "what the system inferred" for the diagnosis create response.
 */
public record LearnerSnapshotDto(
    @JsonProperty("summary")
    String summary,
    @JsonProperty("signals")
    List<String> signals,
    @JsonProperty("riskTags")
    List<String> riskTags
) {
    public LearnerSnapshotDto {
        signals = signals == null ? List.of() : List.copyOf(signals);
        riskTags = riskTags == null ? List.of() : List.copyOf(riskTags);
    }
}
