package com.pandanav.learning.api.dto.diagnosis;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Meta for debugging / demo: personalization level and signals used.
 */
public record PersonalizationMetaDto(
    @JsonProperty("personalizationLevel")
    String personalizationLevel,
    @JsonProperty("usedSignals")
    List<String> usedSignals,
    @JsonProperty("questionSelectionMode")
    String questionSelectionMode
) {
    public PersonalizationMetaDto {
        usedSignals = usedSignals == null ? List.of() : List.copyOf(usedSignals);
    }
}
