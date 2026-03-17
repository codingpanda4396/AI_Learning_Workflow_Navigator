package navigator.application.diagnosis;

import navigator.domain.enums.ConfidenceLevel;
import navigator.domain.enums.ExecutionStability;
import navigator.domain.enums.FoundationLevel;
import navigator.domain.enums.LearningPreference;
import navigator.domain.enums.TimeBudget;
import navigator.domain.enums.UrgencyLevel;
import navigator.domain.model.GoalContextSnapshot;
import navigator.domain.model.LearnerProfileSnapshot;
import navigator.domain.model.StructuredLearningGoal;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Sprint 2.5: answers + goal + goalContext -> LearnerProfileSnapshot V2（6 维收敛）。
 */
@Component
public class DiagnosisRuleEngine {

    private static final List<String> GAP_TYPES = List.of(
            "CONCEPT_GAP", "RELATIONSHIP_GAP", "PROCEDURE_GAP",
            "QUESTION_TYPE_RECOGNITION_GAP", "EXPRESSION_GAP"
    );

    public LearnerProfileSnapshot buildProfile(
            String diagnosisId,
            DiagnosisAnswerNormalizer.NormalizedAnswers normalized,
            StructuredLearningGoal goal,
            GoalContextSnapshot goalContext) {
        FoundationResult foundation = mapFoundation(normalized.getFoundationCode());
        String primaryGap = mapGap(normalized.getGapCodes() != null && !normalized.getGapCodes().isEmpty() ? normalized.getGapCodes().get(0) : null);
        List<String> blockerTags = primaryGap != null ? List.of(primaryGap) : List.of();

        ExecutionStability executionStability = deriveExecutionStability(foundation, primaryGap);
        TimeBudget timeBudgetLevel = goal != null ? goal.getTimeBudget() : null;
        LearningPreference learningPreference = deriveLearningPreference(goal);
        UrgencyLevel urgencyLevel = goal != null ? goal.getUrgencyLevel() : null;
        List<String> riskTags = deriveRiskTags(goalContext, foundation, primaryGap);

        return LearnerProfileSnapshot.builder()
                .diagnosisId(diagnosisId)
                .foundationLevel(foundation.level)
                .executionStability(executionStability)
                .timeBudgetLevel(timeBudgetLevel)
                .learningPreference(learningPreference)
                .blockingPoint(primaryGap)
                .urgencyLevel(urgencyLevel)
                .blockerTags(blockerTags)
                .riskTags(riskTags)
                .build();
    }

    private ExecutionStability deriveExecutionStability(FoundationResult foundation, String primaryGap) {
        boolean unstableGap = "CONCEPT_GAP".equals(primaryGap) || "RELATIONSHIP_GAP".equals(primaryGap);
        boolean lowConfidence = foundation.confidenceLevel == ConfidenceLevel.LOW;
        if (foundation.level == FoundationLevel.BEGINNER || (unstableGap && lowConfidence)) {
            return ExecutionStability.UNSTABLE;
        }
        if (unstableGap || lowConfidence) {
            return ExecutionStability.MODERATE;
        }
        return ExecutionStability.STABLE;
    }

    private LearningPreference deriveLearningPreference(StructuredLearningGoal goal) {
        if (goal == null || goal.getPreferenceTags() == null || goal.getPreferenceTags().isEmpty()) {
            return LearningPreference.BALANCED;
        }
        var tags = goal.getPreferenceTags();
        if (tags.contains(navigator.domain.enums.PreferenceTag.PRACTICE_FIRST)) return LearningPreference.PRACTICE_FIRST;
        if (tags.contains(navigator.domain.enums.PreferenceTag.EXAMPLE_FIRST)) return LearningPreference.EXAMPLE_FIRST;
        if (tags.contains(navigator.domain.enums.PreferenceTag.FRAMEWORK_FIRST)) return LearningPreference.FRAMEWORK_FIRST;
        if (tags.contains(navigator.domain.enums.PreferenceTag.STEP_BY_STEP)) return LearningPreference.STEP_BY_STEP;
        if (tags.contains(navigator.domain.enums.PreferenceTag.CONCEPT_FIRST)) return LearningPreference.CONCEPT_FIRST;
        return LearningPreference.BALANCED;
    }

    private FoundationResult mapFoundation(String code) {
        if (code == null) {
            return new FoundationResult(FoundationLevel.BASIC, false, ConfidenceLevel.MEDIUM);
        }
        switch (code.toUpperCase()) {
            case "BEGINNER":
                return new FoundationResult(FoundationLevel.BEGINNER, true, ConfidenceLevel.LOW);
            case "LEARNED_BUT_FORGOTTEN":
                return new FoundationResult(FoundationLevel.BASIC, true, ConfidenceLevel.LOW);
            case "BASIC":
                return new FoundationResult(FoundationLevel.BASIC, false, ConfidenceLevel.LOW);
            case "PROFICIENT":
                return new FoundationResult(FoundationLevel.INTERMEDIATE, false, ConfidenceLevel.MEDIUM);
            case "CAN_EXPLAIN_BUT_NOT_APPLY":
                return new FoundationResult(FoundationLevel.INTERMEDIATE, false, ConfidenceLevel.MEDIUM);
            case "ADVANCED":
            case "SOLID":
                return new FoundationResult(FoundationLevel.SOLID, false, ConfidenceLevel.HIGH);
            default:
                return new FoundationResult(FoundationLevel.BASIC, false, ConfidenceLevel.MEDIUM);
        }
    }

    private String mapGap(String code) {
        if (code == null || code.isBlank()) return null;
        String u = code.toUpperCase();
        if (GAP_TYPES.contains(u)) return u;
        if ("CONCEPT_UNCLEAR".equals(u)) return "CONCEPT_GAP";
        if ("RELATIONSHIP_CONFUSION".equals(u)) return "RELATIONSHIP_GAP";
        if ("PROCEDURE_NOT_CLEAR".equals(u)) return "PROCEDURE_GAP";
        if ("QUESTION_TYPE_RECOGNITION".equals(u)) return "QUESTION_TYPE_RECOGNITION_GAP";
        if ("CANNOT_EXPLAIN_CLEARY".equals(u) || "EXPRESSION_NOT_CLEAR".equals(u)) return "EXPRESSION_GAP";
        if ("STRUCTURE_GAP".equals(u)) return "RELATIONSHIP_GAP";
        if ("APPLICATION_GAP".equals(u)) return "PROCEDURE_GAP";
        return u;
    }

    private List<String> deriveRiskTags(GoalContextSnapshot goalContext, FoundationResult foundation, String primaryGap) {
        List<String> tags = new ArrayList<>();
        if (goalContext != null && goalContext.getRiskTags() != null && goalContext.getRiskTags().contains("TIME_PRESSURE")) {
            tags.add("TIME_PRESSURE");
        }
        if (foundation.level == FoundationLevel.BEGINNER || foundation.prerequisiteGap) {
            tags.add("PREREQUISITE_GAP");
        }
        if (foundation.level == FoundationLevel.SOLID && (primaryGap == null || "CONCEPT_GAP".equals(primaryGap))) {
            tags.add("OVERCONFIDENCE_RISK");
        }
        if (goalContext != null && goalContext.getRiskTags() != null && goalContext.getRiskTags().stream().anyMatch(r -> r.contains("TIME")) && ("CONCEPT_GAP".equals(primaryGap) || "RELATIONSHIP_GAP".equals(primaryGap))) {
            tags.add("SHALLOW_UNDERSTANDING_RISK");
        }
        return tags.isEmpty() ? List.of() : tags;
    }

    private static class FoundationResult {
        final FoundationLevel level;
        final boolean prerequisiteGap;
        final ConfidenceLevel confidenceLevel;

        FoundationResult(FoundationLevel level, boolean prerequisiteGap, ConfidenceLevel confidenceLevel) {
            this.level = level;
            this.prerequisiteGap = prerequisiteGap;
            this.confidenceLevel = confidenceLevel;
        }
    }
}
