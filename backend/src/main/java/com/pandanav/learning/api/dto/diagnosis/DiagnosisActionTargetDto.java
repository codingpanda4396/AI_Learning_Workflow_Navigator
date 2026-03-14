package com.pandanav.learning.api.dto.diagnosis;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public record DiagnosisActionTargetDto(
    @JsonProperty("route")
    String route,
    @JsonProperty("params")
    Map<String, Object> params
) {
}
