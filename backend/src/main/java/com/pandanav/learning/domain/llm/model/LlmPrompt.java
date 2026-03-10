package com.pandanav.learning.domain.llm.model;

public record LlmPrompt(
    PromptTemplateKey templateKey,
    String promptKey,
    String promptVersion,
    LlmInvocationProfile invocationProfile,
    String systemPrompt,
    String userPrompt,
    String expectedJsonSchemaText,
    String outputRules,
    String modelHint,
    Integer maxOutputTokens
) {
}
