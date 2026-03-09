package com.pandanav.learning.domain.llm.model;

import java.util.List;

public record ConceptNodeDecomposeResult(
    List<ConceptNodeItem> conceptNodes
) {
    public record ConceptNodeItem(
        String id,
        String title,
        String description,
        List<String> prerequisites
    ) {
    }
}
