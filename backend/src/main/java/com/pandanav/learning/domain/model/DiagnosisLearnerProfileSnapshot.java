package com.pandanav.learning.domain.model;

import java.util.List;

/**
 * Snapshot of learner profile before diagnosis (pre-submit).
 * Used to drive strategy decision and question selection; distinct from
 * {@link LearnerProfileSnapshot} which is built after diagnosis submission.
 */
public record DiagnosisLearnerProfileSnapshot(
    String learnerStage,
    String goalClarity,
    String timeConstraint,
    String confidenceLevel,
    List<String> weaknessTags,
    List<String> behaviorSignals,
    List<String> evidence,
    boolean hasHistory,
    boolean hasRecentFailures,
    boolean hasContradictionRisk
) {
    public DiagnosisLearnerProfileSnapshot {
        learnerStage = learnerStage == null ? "BASIC" : learnerStage;
        goalClarity = goalClarity == null ? "MEDIUM" : goalClarity;
        timeConstraint = timeConstraint == null ? "MEDIUM" : timeConstraint;
        confidenceLevel = confidenceLevel == null ? "MEDIUM" : confidenceLevel;
        weaknessTags = weaknessTags == null ? List.of() : List.copyOf(weaknessTags);
        behaviorSignals = behaviorSignals == null ? List.of() : List.copyOf(behaviorSignals);
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
    }
}
