package com.pandanav.learning.domain.llm.model;

public enum PromptTemplateKey {
    STRUCTURE_V1("STRUCTURE", "v1"),
    STRUCTURE_V1_1("STRUCTURE", "v1.1"),
    UNDERSTANDING_V1("UNDERSTANDING", "v1"),
    TRAINING_V1("TRAINING", "v1"),
    REFLECTION_V1("REFLECTION", "v1"),
    EVALUATE_V1("EVALUATE", "v1"),
    EVALUATE_V2("EVALUATE", "v2"),
    GOAL_DIAGNOSE_V1("GOAL_DIAGNOSE", "v1"),
    DIAGNOSIS_QUESTION_V1("DIAGNOSIS_QUESTION", "v1"),
    CAPABILITY_SUMMARY_V1("CAPABILITY_SUMMARY", "v1"),
    PATH_PLAN_V1("PATH_PLAN", "v1"),
    LEARNING_PLAN_V1("LEARNING_PLAN", "v1"),
    LEARNING_PLAN_V2("LEARNING_PLAN", "v2"),
    LEARNING_PLAN_DECISION_V1("LEARNING_PLAN_DECISION", "v1"),
    PRACTICE_GENERATION_V1("PRACTICE_GENERATION", "v1"),
    TUTOR_V1("TUTOR", "v1"),
    CONCEPT_DECOMPOSE_V1("CONCEPT_DECOMPOSE", "v1");

    private final String promptKey;
    private final String promptVersion;

    PromptTemplateKey(String promptKey, String promptVersion) {
        this.promptKey = promptKey;
        this.promptVersion = promptVersion;
    }

    public String promptKey() {
        return promptKey;
    }

    public String promptVersion() {
        return promptVersion;
    }
}
