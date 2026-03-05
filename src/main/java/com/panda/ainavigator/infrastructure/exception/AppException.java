package com.panda.ainavigator.infrastructure.exception;

public class AppException extends RuntimeException {
    private final ApiErrorCode error;

    public AppException(ApiErrorCode error, String message) {
        super(message);
        this.error = error;
    }

    public ApiErrorCode getError() {
        return error;
    }
}
