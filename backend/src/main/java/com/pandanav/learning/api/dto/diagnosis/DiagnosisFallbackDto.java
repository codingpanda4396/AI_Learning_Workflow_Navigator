package com.pandanav.learning.api.dto.diagnosis;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record DiagnosisFallbackDto(
    @JsonProperty("applied")
    boolean applied,
    @JsonProperty("reasons")
    List<String> reasons,
    @JsonProperty("contentSource")
    String contentSource
) {
}
