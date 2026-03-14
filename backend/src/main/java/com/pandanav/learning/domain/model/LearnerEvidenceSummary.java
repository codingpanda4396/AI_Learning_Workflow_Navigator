package com.pandanav.learning.domain.model;

import java.util.List;

public record LearnerEvidenceSummary(
    List<LearnerEvidenceSignal> machineSignals,
    String machineConfidence,
    String machineTrend,
    List<String> topEvidence,
    String whyThisStep,
    String skipRisk,
    String expectedGain,
    String confidenceHint
) {
    public record LearnerEvidenceSignal(
        String signalKey,
        String signalStrength,
        double confidence,
        List<String> evidence,
        String trend
    ) {
    }
}
