package com.pandanav.learning.domain.model;

public record ActionTemplate(
    String stage,
    String title,
    String goal,
    String learnerAction,
    String aiSupport,
    Integer estimatedMinutes
) {
}
