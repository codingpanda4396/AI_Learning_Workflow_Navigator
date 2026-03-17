package navigator.application.goal;

import navigator.domain.enums.GoalType;
import navigator.domain.enums.PlanningMode;
import navigator.domain.enums.SelfReportedLevel;
import navigator.domain.enums.UrgencyLevel;
import navigator.domain.enums.TimeBudget;
import navigator.domain.model.GoalContextSnapshot;
import navigator.domain.model.StructuredLearningGoal;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GoalContextDeriverTest {

    private final GoalContextDeriver deriver = new GoalContextDeriver();

    @Test
    void fastTrackForExamReviewHighUrgency() {
        StructuredLearningGoal goal = StructuredLearningGoal.builder()
                .goalType(GoalType.REVIEW_FOR_EXAM)
                .urgencyLevel(UrgencyLevel.HIGH)
                .topicScopeType("SINGLE_TOPIC")
                .selfReportedLevel(SelfReportedLevel.BASIC)
                .build();
        GoalContextSnapshot ctx = deriver.derive(goal);
        assertThat(ctx.getPlanningMode()).isEqualTo(PlanningMode.EXAM_CRASH);
    }

    @Test
    void remedialForBeginner() {
        StructuredLearningGoal goal = StructuredLearningGoal.builder()
                .goalType(GoalType.LEARN_NEW_CONCEPT)
                .selfReportedLevel(SelfReportedLevel.BEGINNER)
                .topicScopeType("SINGLE_TOPIC")
                .build();
        GoalContextSnapshot ctx = deriver.derive(goal);
        assertThat(ctx.getPlanningMode()).isEqualTo(PlanningMode.STEADY_FOUNDATION);
    }

    @Test
    void systematicPathForCourseScope() {
        StructuredLearningGoal goal = StructuredLearningGoal.builder()
                .goalType(GoalType.BUILD_SYSTEMATIC_UNDERSTANDING)
                .topicScopeType("COURSE")
                .build();
        GoalContextSnapshot ctx = deriver.derive(goal);
        assertThat(ctx.getPlanningMode()).isEqualTo(PlanningMode.SYSTEMATIC_BUILD);
    }
}
