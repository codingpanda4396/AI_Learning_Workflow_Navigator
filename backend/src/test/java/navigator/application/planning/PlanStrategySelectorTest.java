package navigator.application.planning;

import navigator.application.rule.planning.PlanningRule;
import navigator.application.rule.planning.rules.ConceptClarificationRule;
import navigator.application.rule.planning.rules.DrillStrengthenRule;
import navigator.application.rule.planning.rules.FoundationPatchRule;
import navigator.application.rule.planning.rules.FrameworkBuildRule;
import navigator.application.rule.planning.rules.LocalRepairRule;
import navigator.application.rule.planning.rules.SprintCorrectionRule;
import navigator.domain.enums.FoundationLevel;
import navigator.domain.enums.GoalType;
import navigator.domain.enums.UrgencyLevel;
import navigator.domain.model.LearnerProfileSnapshot;
import navigator.domain.model.StructuredLearningGoal;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PlanStrategySelectorTest {

    private final PlanStrategySelector selector = new PlanStrategySelector(defaultRules());

    @Test
    void systematicProgressiveForCourseScope() {
        PlanningContext ctx = PlanningContext.builder()
                .goal(StructuredLearningGoal.builder().goalType(GoalType.BUILD_SYSTEMATIC_UNDERSTANDING).topicScopeType("COURSE").build())
                .learnerProfileSnapshot(LearnerProfileSnapshot.builder().foundationLevel(FoundationLevel.BASIC).build())
                .build();
        assertThat(selector.select(ctx)).isEqualTo(PlanStrategySelector.FRAMEWORK_BUILD);
    }

    @Test
    void foundationRebuildForBeginner() {
        PlanningContext ctx = PlanningContext.builder()
                .goal(StructuredLearningGoal.builder().goalType(GoalType.LEARN_NEW_CONCEPT).topicScopeType("SINGLE_TOPIC").build())
                .learnerProfileSnapshot(LearnerProfileSnapshot.builder().foundationLevel(FoundationLevel.BEGINNER).riskTags(List.of("PREREQUISITE_GAP")).build())
                .build();
        assertThat(selector.select(ctx)).isEqualTo(PlanStrategySelector.FOUNDATION_PATCH);
    }

    @Test
    void compressedReviewForExamHighUrgency() {
        PlanningContext ctx = PlanningContext.builder()
                .goal(StructuredLearningGoal.builder().goalType(GoalType.REVIEW_FOR_EXAM).urgencyLevel(UrgencyLevel.HIGH).topicScopeType("SINGLE_TOPIC").build())
                .learnerProfileSnapshot(LearnerProfileSnapshot.builder().foundationLevel(FoundationLevel.BASIC).blockerTags(List.of("QUESTION_TYPE_RECOGNITION_GAP")).build())
                .build();
        assertThat(selector.select(ctx)).isEqualTo(PlanStrategySelector.SPRINT_CORRECTION);
    }

    @Test
    void localRepairForBlockerSingleTopic() {
        PlanningContext ctx = PlanningContext.builder()
                .goal(StructuredLearningGoal.builder().goalType(GoalType.FIX_SPECIFIC_BLOCKER).topicScopeType("SINGLE_TOPIC").build())
                .learnerProfileSnapshot(LearnerProfileSnapshot.builder().foundationLevel(FoundationLevel.BASIC).blockerTags(List.of("CONCEPT_GAP")).build())
                .build();
        assertThat(selector.select(ctx)).isEqualTo(PlanStrategySelector.LOCAL_REPAIR);
    }

    @Test
    void conceptClarificationFallback() {
        PlanningContext ctx = PlanningContext.builder()
                .goal(StructuredLearningGoal.builder().goalType(GoalType.LEARN_NEW_CONCEPT).topicScopeType("SINGLE_TOPIC").build())
                .learnerProfileSnapshot(LearnerProfileSnapshot.builder().foundationLevel(FoundationLevel.BASIC).blockerTags(List.of()).build())
                .build();
        assertThat(selector.select(ctx)).isEqualTo(PlanStrategySelector.CONCEPT_CLARIFICATION);
    }

    @Test
    void selectResultShouldExposeRuleMetadata() {
        PlanningContext ctx = PlanningContext.builder()
                .goal(StructuredLearningGoal.builder().goalType(GoalType.FIX_SPECIFIC_BLOCKER).topicScopeType("SINGLE_TOPIC").build())
                .learnerProfileSnapshot(LearnerProfileSnapshot.builder().foundationLevel(FoundationLevel.BASIC).build())
                .build();

        var result = selector.selectResult(ctx);

        assertThat(result.getResult()).isEqualTo(PlanStrategySelector.LOCAL_REPAIR);
        assertThat(result.getRuleId()).isEqualTo("LOCAL_REPAIR_RULE");
        assertThat(result.getReason()).isEqualTo("Single-topic blocker should use local repair");
    }

    private static List<PlanningRule> defaultRules() {
        return List.of(
                new ConceptClarificationRule(),
                new LocalRepairRule(),
                new DrillStrengthenRule(),
                new SprintCorrectionRule(),
                new FoundationPatchRule(),
                new FrameworkBuildRule()
        );
    }
}
