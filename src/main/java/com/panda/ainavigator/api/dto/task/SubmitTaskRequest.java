package com.panda.ainavigator.api.dto.task;

import jakarta.validation.constraints.NotBlank;

public record SubmitTaskRequest(@NotBlank String user_answer) {
}
