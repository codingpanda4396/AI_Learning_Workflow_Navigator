package com.pandanav.learning.domain.llm.model;

public class LlmCallException extends RuntimeException {

    private final LlmFailureType failureType;

    public LlmCallException(LlmFailureType failureType, String message) {
        super(message);
        this.failureType = failureType;
    }

    public LlmCallException(LlmFailureType failureType, String message, Throwable cause) {
        super(message, cause);
        this.failureType = failureType;
    }

    public LlmFailureType failureType() {
        return failureType;
    }
}
