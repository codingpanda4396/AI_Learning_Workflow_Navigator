package navigator.application.rule.planning;

import navigator.application.planning.PlanStrategySelector;
import navigator.application.planning.PlanningContext;
import navigator.application.rule.engine.RuleEngine;
import navigator.application.rule.engine.RuleResult;
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

class PlanningRuleTest {

    @Test
    void frameworkBuildRuleShouldMatchCourseScope() {
        PlanningRule rule = new FrameworkBuildRule();

        assertThat(rule.match(context(GoalType.LEARN_NEW_CONCEPT, "COURSE", FoundationLevel.BASIC, null, null, null))).isTrue();
        assertThat(rule.apply(null)).isEqualTo(PlanStrategySelector.FRAMEWORK_BUILD);
    }

    @Test
    void foundationPatchRuleShouldMatchBeginnerOrPrerequisiteGap() {
        PlanningRule rule = new FoundationPatchRule();

        assertThat(rule.match(context(GoalType.LEARN_NEW_CONCEPT, "SINGLE_TOPIC", FoundationLevel.BEGINNER, null, null, null))).isTrue();
        assertThat(rule.match(context(GoalType.LEARN_NEW_CONCEPT, "SINGLE_TOPIC", FoundationLevel.BASIC, null, List.of("PREREQUISITE_GAP"), null))).isTrue();
        assertThat(rule.reason(null)).isEqualTo("Beginner foundation or prerequisite gap requires a foundation patch");
    }

    @Test
    void sprintCorrectionRuleShouldMatchUrgentExamReview() {
        PlanningRule rule = new SprintCorrectionRule();

        assertThat(rule.match(context(GoalType.REVIEW_FOR_EXAM, "SINGLE_TOPIC", FoundationLevel.BASIC, UrgencyLevel.HIGH, null, null))).isTrue();
        assertThat(rule.match(context(GoalType.REVIEW_FOR_EXAM, "SINGLE_TOPIC", FoundationLevel.BEGINNER, UrgencyLevel.HIGH, null, null))).isFalse();
    }

    @Test
    void drillStrengthenRuleShouldMatchProcedureOrPracticeEnhancement() {
        PlanningRule rule = new DrillStrengthenRule();

        assertThat(rule.match(context(GoalType.LEARN_NEW_CONCEPT, "SINGLE_TOPIC", FoundationLevel.BASIC, null, null, List.of("PROCEDURE_GAP")))).isTrue();
        assertThat(rule.match(context(GoalType.PRACTICE_ENHANCEMENT, "SINGLE_TOPIC", FoundationLevel.BASIC, null, null, null))).isTrue();
    }

    @Test
    void localRepairRuleShouldMatchSingleTopicBlockerFix() {
        PlanningRule rule = new LocalRepairRule();

        assertThat(rule.match(context(GoalType.FIX_SPECIFIC_BLOCKER, "SINGLE_TOPIC", FoundationLevel.BASIC, null, null, null))).isTrue();
        assertThat(rule.match(context(GoalType.FIX_SPECIFIC_BLOCKER, "CHAPTER", FoundationLevel.BASIC, null, null, null))).isFalse();
    }

    @Test
    void conceptClarificationRuleShouldActAsFallback() {
        PlanningRule rule = new ConceptClarificationRule();

        assertThat(rule.match(context(GoalType.LEARN_NEW_CONCEPT, "SINGLE_TOPIC", FoundationLevel.BASIC, null, null, null))).isTrue();
        assertThat(rule.apply(null)).isEqualTo(PlanStrategySelector.CONCEPT_CLARIFICATION);
    }

    @Test
    void engineShouldPreserveLegacyPriorityOrdering() {
        RuleEngine<PlanningContext, String> engine = new RuleEngine<>(List.of(
                new ConceptClarificationRule(),
                new LocalRepairRule(),
                new DrillStrengthenRule(),
                new SprintCorrectionRule(),
                new FoundationPatchRule(),
                new FrameworkBuildRule()
        ));
        PlanningContext context = context(
                GoalType.REVIEW_FOR_EXAM,
                "SINGLE_TOPIC",
                FoundationLevel.BASIC,
                UrgencyLevel.HIGH,
                null,
                List.of("QUESTION_TYPE_RECOGNITION_GAP")
        );

        RuleResult<String> result = engine.execute(context);

        assertThat(result.getResult()).isEqualTo(PlanStrategySelector.SPRINT_CORRECTION);
        assertThat(result.getRuleId()).isEqualTo("SPRINT_CORRECTION_RULE");
    }

    private static PlanningContext context(GoalType goalType,
                                           String topicScopeType,
                                           FoundationLevel foundationLevel,
                                           UrgencyLevel urgencyLevel,
                                           List<String> riskTags,
                                           List<String> blockerTags) {
        return PlanningContext.builder()
                .goal(StructuredLearningGoal.builder()
                        .goalType(goalType)
                        .topicScopeType(topicScopeType)
                        .urgencyLevel(urgencyLevel)
                        .build())
                .learnerProfileSnapshot(LearnerProfileSnapshot.builder()
                        .foundationLevel(foundationLevel)
                        .riskTags(riskTags)
                        .blockerTags(blockerTags)
                        .build())
                .build();
    }
}
