package com.panda.ainavigator.api.dto.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.panda.ainavigator.infrastructure.exception.ApiErrorCode;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(ApiErrorCode error, String message) {
}
