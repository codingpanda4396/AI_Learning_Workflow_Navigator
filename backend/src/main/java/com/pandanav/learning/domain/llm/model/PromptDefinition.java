package com.pandanav.learning.domain.llm.model;

public record PromptDefinition(
    PromptTemplateKey templateKey,
    PromptSpec spec
) {
}
