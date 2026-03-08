package com.pandanav.learning.domain.llm.model;

public record LlmPrompt(
    PromptTemplateKey templateKey,
    String promptVersion,
    String systemPrompt,
    String userPrompt
) {
}

