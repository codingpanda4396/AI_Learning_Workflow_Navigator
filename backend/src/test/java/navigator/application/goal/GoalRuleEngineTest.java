package navigator.application.goal;

import navigator.domain.enums.GoalType;
import navigator.domain.enums.SelfReportedLevel;
import navigator.domain.enums.TimeBudget;
import navigator.domain.enums.UrgencyLevel;
import navigator.domain.model.LearningGoalInput;
import navigator.domain.model.StructuredLearningGoal;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GoalRuleEngineTest {

    private final GoalRuleEngine engine = new GoalRuleEngine();

    @Test
    void examReviewGoal() {
        LearningGoalInput input = LearningGoalInput.builder()
                .rawGoalText("我明天考试，想快速搞懂栈和队列，最好能会做题")
                .timeBudget(TimeBudget.WITHIN_30_MIN)
                .selfReportedLevel(SelfReportedLevel.BASIC)
                .build();
        StructuredLearningGoal goal = engine.derive(input);
        assertThat(goal.getGoalType()).isEqualTo(GoalType.REVIEW_FOR_EXAM);
        assertThat(goal.getTopicScopeType()).isEqualTo("MULTI_TOPIC");
        assertThat(goal.getUrgencyLevel()).isEqualTo(UrgencyLevel.HIGH);
        assertThat(goal.getTopics()).containsAnyOf("栈", "队列");
    }

    @Test
    void newConceptGoal() {
        LearningGoalInput input = LearningGoalInput.builder()
                .rawGoalText("我想搞懂链表")
                .timeBudget(TimeBudget.MULTI_DAY)
                .selfReportedLevel(SelfReportedLevel.BEGINNER)
                .build();
        StructuredLearningGoal goal = engine.derive(input);
        assertThat(goal.getGoalType()).isEqualTo(GoalType.LEARN_NEW_CONCEPT);
        assertThat(goal.getTopicScopeType()).isEqualTo("SINGLE_TOPIC");
        assertThat(goal.getTopics()).isNotEmpty();
    }

    @Test
    void systematicGoal() {
        LearningGoalInput input = LearningGoalInput.builder()
                .rawGoalText("我想系统学习 408 数据结构")
                .timeBudget(TimeBudget.LONG_TERM)
                .selfReportedLevel(SelfReportedLevel.BASIC)
                .build();
        StructuredLearningGoal goal = engine.derive(input);
        assertThat(goal.getGoalType()).isEqualTo(GoalType.BUILD_SYSTEMATIC_UNDERSTANDING);
        assertThat(goal.getTopicScopeType()).isEqualTo("COURSE");
        assertThat(goal.getSubject()).isEqualTo("COMPUTER_408");
    }
}
