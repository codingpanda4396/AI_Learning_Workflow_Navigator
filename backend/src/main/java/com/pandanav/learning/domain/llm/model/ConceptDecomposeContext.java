package com.pandanav.learning.domain.llm.model;

public record ConceptDecomposeContext(
    String chapterId,
    String concept,
    String goal
) {
}
