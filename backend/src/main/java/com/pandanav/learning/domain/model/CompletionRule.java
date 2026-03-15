package com.pandanav.learning.domain.model;

import java.util.List;

public record CompletionRule(
    String ruleType,
    Integer threshold,
    List<String> requiredEvidenceTypes,
    Integer maxAttempts
) {
}

