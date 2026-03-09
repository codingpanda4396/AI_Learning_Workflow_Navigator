package com.pandanav.learning.domain.llm.model;

public record PromptSpec(
    String promptKey,
    String promptVersion,
    String systemPrompt,
    String userPromptTemplate,
    String expectedJsonSchemaText,
    String outputRules,
    String modelHint
) {
}
