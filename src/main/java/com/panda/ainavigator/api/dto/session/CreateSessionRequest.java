package com.panda.ainavigator.api.dto.session;

import jakarta.validation.constraints.NotBlank;

public record CreateSessionRequest(
        @NotBlank String user_id,
        @NotBlank String course_id,
        @NotBlank String chapter_id,
        String goal_text
) {
}
