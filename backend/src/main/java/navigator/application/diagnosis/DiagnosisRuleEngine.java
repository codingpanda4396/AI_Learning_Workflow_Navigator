package navigator.application.diagnosis;

import navigator.domain.enums.FoundationLevel;
import navigator.domain.enums.ConfidenceLevel;
import navigator.domain.enums.GoalType;
import navigator.domain.model.GoalContextSnapshot;
import navigator.domain.model.LearnerProfileSnapshot;
import navigator.domain.model.StructuredLearningGoal;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Sprint 1: answers + goalContext -> LearnerProfileSnapshot（foundation/gap 映射 + 组合矩阵）。
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
        String secondaryGap = normalized.getGapCodes() != null && normalized.getGapCodes().size() > 1 ? mapGap(normalized.getGapCodes().get(1)) : null;

        String entryStrategy = deriveEntryStrategy(goal, foundation, primaryGap);
        String granularity = deriveGranularity(goal, foundation, primaryGap);
        String feedbackFreq = deriveFeedbackMode(goal, foundation, primaryGap);
        List<String> riskTags = deriveRiskTags(goalContext, foundation, primaryGap);
        List<String> planningHints = List.of("根据诊断与目标生成");

        return LearnerProfileSnapshot.builder()
                .diagnosisId(diagnosisId)
                .foundationLevel(foundation.level)
                .confidenceLevel(foundation.confidenceLevel)
                .comprehensionPattern(foundation.level.name())
                .executionPattern(primaryGap != null ? primaryGap : "GENERAL")
                .blockerTags(primaryGap != null ? List.of(primaryGap) : List.of())
                .riskTags(riskTags)
                .suggestedEntryStrategy(entryStrategy)
                .suggestedGranularity(granularity)
                .suggestedFeedbackFrequency(feedbackFreq)
                .planningHints(planningHints)
                .build();
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

    private String deriveEntryStrategy(StructuredLearningGoal goal, FoundationResult foundation, String primaryGap) {
        GoalType goalType = goal != null ? goal.getGoalType() : null;
        String scope = goal != null ? goal.getTopicScopeType() : null;
        boolean chapterOrCourse = "CHAPTER".equals(scope) || "COURSE".equals(scope);

        if (goalType == GoalType.REVIEW_FOR_EXAM && isProcedureOrQuestionTypeGap(primaryGap) && foundation.level != FoundationLevel.BEGINNER) {
            return "CORE_CONTRAST_FIRST";
        }
        if (foundation.level == FoundationLevel.BEGINNER || foundation.prerequisiteGap) {
            return "PREREQUISITE_FIRST";
        }
        if (goalType == GoalType.BUILD_SYSTEMATIC_UNDERSTANDING || (chapterOrCourse && "RELATIONSHIP_GAP".equals(primaryGap))) {
            return "FRAMEWORK_THEN_FILL";
        }
        if (goalType == GoalType.PRACTICE_ENHANCEMENT || isProcedureOrQuestionTypeGap(primaryGap)) {
            return "EXAMPLE_TO_PRACTICE";
        }
        if (goalType == GoalType.FIX_SPECIFIC_BLOCKER && "SINGLE_TOPIC".equals(scope)) {
            return "BLOCKER_FIRST";
        }
        return "CONCEPT_FIRST";
    }

    private boolean isProcedureOrQuestionTypeGap(String gap) {
        return "QUESTION_TYPE_RECOGNITION_GAP".equals(gap) || "PROCEDURE_GAP".equals(gap);
    }

    private String deriveGranularity(StructuredLearningGoal goal, FoundationResult foundation, String primaryGap) {
        if (foundation.level == FoundationLevel.BEGINNER || foundation.prerequisiteGap) return "SMALL";
        if (goal != null && ("CHAPTER".equals(goal.getTopicScopeType()) || "COURSE".equals(goal.getTopicScopeType()))) return "NORMAL";
        return "SMALL";
    }

    private String deriveFeedbackMode(StructuredLearningGoal goal, FoundationResult foundation, String primaryGap) {
        if (goal != null && goal.getGoalType() == GoalType.REVIEW_FOR_EXAM && isProcedureOrQuestionTypeGap(primaryGap) && foundation.level != FoundationLevel.BEGINNER) {
            return "FREQUENT_CHECKPOINT";
        }
        if (foundation.level == FoundationLevel.BEGINNER || foundation.prerequisiteGap) return "FREQUENT_CHECKPOINT";
        if (goal != null && (goal.getGoalType() == GoalType.BUILD_SYSTEMATIC_UNDERSTANDING || "CHAPTER".equals(goal.getTopicScopeType()) || "COURSE".equals(goal.getTopicScopeType()))) {
            return "STAGE_CHECKPOINT";
        }
        if (goal != null && (goal.getGoalType() == GoalType.PRACTICE_ENHANCEMENT || isProcedureOrQuestionTypeGap(primaryGap))) {
            return "FREQUENT_CHECKPOINT";
        }
        return "NORMAL_CHECKPOINT";
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
