package com.pandanav.learning.api.dto.session;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record PathOptionsResponse(
    @JsonProperty("path_options")
    List<PathOptionResponse> pathOptions
) {
    public record PathOptionResponse(
        @JsonProperty("path_id")
        String pathId,
        String name,
        String description,
        String difficulty,
        @JsonProperty("estimated_minutes")
        Integer estimatedMinutes,
        List<String> steps
    ) {
    }
}

