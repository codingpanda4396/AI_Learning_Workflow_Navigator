package com.pandanav.learning.domain.model;

public record DiagnosisSignal(
    String featureKey,
    String featureValue,
    double scoreDelta,
    double confidence,
    String evidence
) {
}
