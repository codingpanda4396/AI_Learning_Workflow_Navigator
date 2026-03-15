package com.pandanav.learning.domain.model;

import java.util.List;

/**
 * Snapshot of learner profile before diagnosis (pre-submit).
 * Used to drive strategy decision and question selection; distinct from
 * {@link LearnerProfileSnapshot} which is built after diagnosis submission.
 * Enhanced with riskTags and historySignals for LLM selection.
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
    boolean hasContradictionRisk,
    List<String> riskTags,
    List<String> historySignals
) {
    public DiagnosisLearnerProfileSnapshot {
        learnerStage = learnerStage == null ? "BASIC" : learnerStage;
        goalClarity = goalClarity == null ? "MEDIUM" : goalClarity;
        timeConstraint = timeConstraint == null ? "MEDIUM" : timeConstraint;
        confidenceLevel = confidenceLevel == null ? "MEDIUM" : confidenceLevel;
        weaknessTags = weaknessTags == null ? List.of() : List.copyOf(weaknessTags);
        behaviorSignals = behaviorSignals == null ? List.of() : List.copyOf(behaviorSignals);
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
        riskTags = riskTags == null ? List.of() : List.copyOf(riskTags);
        historySignals = historySignals == null ? List.of() : List.copyOf(historySignals);
    }

    /** Alias for LLM context: learning stage. */
    public String learningStage() {
        return learnerStage();
    }
}
