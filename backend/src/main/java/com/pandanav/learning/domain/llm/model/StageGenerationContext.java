package com.pandanav.learning.domain.llm.model;

import com.pandanav.learning.domain.enums.Stage;

import java.math.BigDecimal;

public record StageGenerationContext(
    Long taskId,
    Long sessionId,
    String chapterId,
    Long nodeId,
    String nodeTitle,
    Stage stage,
    String objective,
    String prerequisiteSummary,
    BigDecimal masteryHistory
) {
}

