package com.pandanav.learning.api.dto.session;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(name = "CreateSessionResponse")
public record CreateSessionResponse(
    @JsonProperty("session_id")
    @Schema(name = "session_id", example = "123")
    Long sessionId
) {
}
