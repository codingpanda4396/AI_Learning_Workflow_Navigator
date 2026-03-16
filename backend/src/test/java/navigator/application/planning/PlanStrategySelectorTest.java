package navigator.application.planning;

import navigator.domain.enums.FoundationLevel;
import navigator.domain.enums.GoalType;
import navigator.domain.model.GoalContextSnapshot;
import navigator.domain.model.LearnerProfileSnapshot;
import navigator.domain.model.StructuredLearningGoal;
import org.junit.jupiter.api.Test;

import java.util.List;

import static navigator.application.planning.PlanStrategySelector.*;
import static org.assertj.core.api.Assertions.assertThat;

class PlanStrategySelectorTest {

    private final PlanStrategySelector selector = new PlanStrategySelector();

    @Test
    void systematicProgressiveForCourseScope() {
        PlanningContext ctx = PlanningContext.builder()
                .goal(StructuredLearningGoal.builder().goalType(GoalType.BUILD_SYSTEMATIC_UNDERSTANDING).topicScopeType("COURSE").build())
                .learnerProfileSnapshot(LearnerProfileSnapshot.builder().foundationLevel(FoundationLevel.BASIC).build())
                .build();
        assertThat(selector.select(ctx)).isEqualTo(SYSTEMATIC_PROGRESSIVE);
    }

    @Test
    void foundationRebuildForBeginner() {
        PlanningContext ctx = PlanningContext.builder()
                .goal(StructuredLearningGoal.builder().goalType(GoalType.LEARN_NEW_CONCEPT).topicScopeType("SINGLE_TOPIC").build())
                .learnerProfileSnapshot(LearnerProfileSnapshot.builder().foundationLevel(FoundationLevel.BEGINNER).riskTags(List.of("PREREQUISITE_GAP")).build())
                .build();
        assertThat(selector.select(ctx)).isEqualTo(FOUNDATION_REBUILD);
    }

    @Test
    void compressedReviewForExamHighUrgency() {
        PlanningContext ctx = PlanningContext.builder()
                .goal(StructuredLearningGoal.builder().goalType(GoalType.REVIEW_FOR_EXAM).urgencyLevel("HIGH").topicScopeType("SINGLE_TOPIC").build())
                .learnerProfileSnapshot(LearnerProfileSnapshot.builder().foundationLevel(FoundationLevel.BASIC).blockerTags(List.of("QUESTION_TYPE_RECOGNITION_GAP")).build())
                .build();
        assertThat(selector.select(ctx)).isEqualTo(COMPRESSED_REVIEW);
    }
}
