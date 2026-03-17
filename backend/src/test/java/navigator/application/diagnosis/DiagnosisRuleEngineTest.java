package navigator.application.diagnosis;

import navigator.domain.enums.ExecutionStability;
import navigator.domain.enums.PreferenceTag;
import navigator.domain.enums.FoundationLevel;
import navigator.domain.enums.LearningPreference;
import navigator.domain.enums.TimeBudget;
import navigator.domain.enums.UrgencyLevel;
import navigator.domain.model.GoalContextSnapshot;
import navigator.domain.model.LearnerProfileSnapshot;
import navigator.domain.model.StructuredLearningGoal;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DiagnosisRuleEngineTest {

    private final DiagnosisRuleEngine engine = new DiagnosisRuleEngine();

    @Test
    void beginnerWithConceptGap_producesUnstableAndFoundationBeginner() {
        var normalized = new DiagnosisAnswerNormalizer.NormalizedAnswers(null, "BEGINNER", "CONCEPT_GAP", null, null, null);
        StructuredLearningGoal goal = StructuredLearningGoal.builder()
                .timeBudget(TimeBudget.WITHIN_30_MIN)
                .urgencyLevel(UrgencyLevel.MEDIUM)
                .preferenceTags(List.of())
                .build();
        GoalContextSnapshot goalContext = GoalContextSnapshot.builder().riskTags(List.of()).build();

        LearnerProfileSnapshot profile = engine.buildProfile("diag_1", normalized, goal, goalContext);

        assertThat(profile.getDiagnosisId()).isEqualTo("diag_1");
        assertThat(profile.getFoundationLevel()).isEqualTo(FoundationLevel.BEGINNER);
        assertThat(profile.getExecutionStability()).isEqualTo(ExecutionStability.UNSTABLE);
        assertThat(profile.getTimeBudgetLevel()).isEqualTo(TimeBudget.WITHIN_30_MIN);
        assertThat(profile.getUrgencyLevel()).isEqualTo(UrgencyLevel.MEDIUM);
        assertThat(profile.getBlockingPoint()).isEqualTo("CONCEPT_GAP");
        assertThat(profile.getBlockerTags()).containsExactly("CONCEPT_GAP");
        assertThat(profile.getRiskTags()).contains("PREREQUISITE_GAP");
    }

    @Test
    void basicWithProcedureGap_producesModerateStability() {
        var normalized = new DiagnosisAnswerNormalizer.NormalizedAnswers(null, "BASIC", "PROCEDURE_GAP", null, null, null);
        StructuredLearningGoal goal = StructuredLearningGoal.builder()
                .timeBudget(TimeBudget.MULTI_DAY)
                .urgencyLevel(UrgencyLevel.MEDIUM)
                .preferenceTags(List.of(PreferenceTag.PRACTICE_FIRST))
                .build();

        LearnerProfileSnapshot profile = engine.buildProfile("diag_2", normalized, goal, null);

        assertThat(profile.getFoundationLevel()).isEqualTo(FoundationLevel.BASIC);
        assertThat(profile.getExecutionStability()).isEqualTo(ExecutionStability.MODERATE);
        assertThat(profile.getLearningPreference()).isEqualTo(LearningPreference.PRACTICE_FIRST);
        assertThat(profile.getBlockingPoint()).isEqualTo("PROCEDURE_GAP");
    }

    @Test
    void proficientWithNoGap_producesStableAndBalanced() {
        var normalized = new DiagnosisAnswerNormalizer.NormalizedAnswers(null, "PROFICIENT", null, null, null, null);
        StructuredLearningGoal goal = StructuredLearningGoal.builder()
                .timeBudget(TimeBudget.LONG_TERM)
                .urgencyLevel(UrgencyLevel.LOW)
                .preferenceTags(List.of())
                .build();

        LearnerProfileSnapshot profile = engine.buildProfile("diag_3", normalized, goal, null);

        assertThat(profile.getFoundationLevel()).isEqualTo(FoundationLevel.INTERMEDIATE);
        assertThat(profile.getExecutionStability()).isEqualTo(ExecutionStability.STABLE);
        assertThat(profile.getLearningPreference()).isEqualTo(LearningPreference.BALANCED);
        assertThat(profile.getBlockingPoint()).isNull();
        assertThat(profile.getBlockerTags()).isEmpty();
    }

    @Test
    void nullGoal_stillProducesProfile() {
        var normalized = new DiagnosisAnswerNormalizer.NormalizedAnswers(null, "BASIC", "CONCEPT_GAP", null, null, null);
        LearnerProfileSnapshot profile = engine.buildProfile("diag_4", normalized, null, null);

        assertThat(profile.getTimeBudgetLevel()).isNull();
        assertThat(profile.getUrgencyLevel()).isNull();
        assertThat(profile.getLearningPreference()).isEqualTo(LearningPreference.BALANCED);
    }

    @Test
    void preferenceFromDiagnosis_overridesGoalPreference() {
        var normalized = new DiagnosisAnswerNormalizer.NormalizedAnswers(null, "BASIC", "CONCEPT_GAP", null, "CORE_CONTRAST_FIRST", null);
        StructuredLearningGoal goal = StructuredLearningGoal.builder()
                .timeBudget(TimeBudget.WITHIN_30_MIN)
                .urgencyLevel(UrgencyLevel.MEDIUM)
                .preferenceTags(List.of(PreferenceTag.CONCEPT_FIRST))
                .build();

        LearnerProfileSnapshot profile = engine.buildProfile("diag_5", normalized, goal, null);

        assertThat(profile.getLearningPreference()).isEqualTo(LearningPreference.CORE_CONTRAST_FIRST);
    }
}
