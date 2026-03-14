package com.pandanav.learning.infrastructure.exception;

public class AiGenerationException extends RuntimeException {

    private final String stage;
    private final String reason;

    public AiGenerationException(String stage, String reason) {
        super("AI generation failed at stage=" + stage + ", reason=" + reason);
        this.stage = stage;
        this.reason = reason;
    }

    public String getStage() {
        return stage;
    }

    public String getReason() {
        return reason;
    }
}
