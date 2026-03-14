package com.pandanav.learning.domain.model;

public record EntryCandidate(
    String conceptId,
    String conceptName,
    String reason,
    Integer estimatedMinutes,
    String priority
) {
}
