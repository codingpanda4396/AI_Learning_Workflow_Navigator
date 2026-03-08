package com.pandanav.learning.domain.llm.model;

import com.pandanav.learning.domain.enums.Stage;

import java.math.BigDecimal;

public record EvaluationContext(
    Long taskId,
    Long sessionId,
    Long nodeId,
    String taskObjective,
    String generatedQuestionContent,
    String userAnswer,
    BigDecimal masteryBefore,
    Stage stage
) {
}

