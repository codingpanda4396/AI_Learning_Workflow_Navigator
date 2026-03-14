package com.pandanav.learning.domain.model;

public record IntensityCandidate(
    String code,
    String label,
    Integer estimatedMinutes,
    String rationale
) {
}
