package com.panda.ainavigator.infrastructure.schema;

public enum LlmSchemaStage {
    UNDERSTANDING("understanding.json"),
    TRAINING("traning_questions.json"),
    EVALUATION("evaluation.json");

    private final String fileName;

    LlmSchemaStage(String fileName) {
        this.fileName = fileName;
    }

    public String getFileName() {
        return fileName;
    }
}
