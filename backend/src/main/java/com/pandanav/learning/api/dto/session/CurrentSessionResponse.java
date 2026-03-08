package com.pandanav.learning.api.dto.session;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "CurrentSessionResponse")
public record CurrentSessionResponse(
    @JsonProperty("has_active_session")
    @Schema(name = "has_active_session", example = "true")
    boolean hasActiveSession,
    CurrentSessionInfoResponse session
) {
}
