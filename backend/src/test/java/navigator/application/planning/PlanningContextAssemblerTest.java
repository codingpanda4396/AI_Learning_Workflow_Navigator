package navigator.application.planning;

import navigator.domain.enums.EntryGranularity;
import navigator.domain.enums.TimeBudget;
import navigator.domain.model.GoalContextSnapshot;
import navigator.domain.model.StructuredLearningGoal;
import navigator.infrastructure.memory.InMemoryStore;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PlanningContextAssemblerTest {

    @Test
    void arrayVsLinkedListGoal_within30Min_smallGranularity_maxTasksAtLeast4() {
        InMemoryStore store = new InMemoryStore();
        store.getGoals().put(
                "goal_1",
                StructuredLearningGoal.builder()
                        .rawGoalText("我想分清顺序表和链表")
                        .timeBudget(TimeBudget.WITHIN_30_MIN)
                        .build());
        store.getGoalContextSnapshots().put(
                "goal_1",
                GoalContextSnapshot.builder()
                        .entryGranularity(EntryGranularity.SMALL)
                        .build());

        PlanningContextAssembler assembler = new PlanningContextAssembler(store);
        PlanningContext ctx = assembler.assemble("goal_1", "diag_1");

        assertThat(ctx.getTimeBudgetConstraint().getMaxTasks()).isGreaterThanOrEqualTo(4);
    }

    @Test
    void arrayAndLinkedListSynonym_within30Min_maxTasksAtLeast4() {
        InMemoryStore store = new InMemoryStore();
        store.getGoals().put(
                "goal_1",
                StructuredLearningGoal.builder()
                        .rawGoalText("数组和链表有什么区别")
                        .timeBudget(TimeBudget.WITHIN_30_MIN)
                        .build());
        store.getGoalContextSnapshots().put(
                "goal_1",
                GoalContextSnapshot.builder()
                        .entryGranularity(EntryGranularity.SMALL)
                        .build());

        PlanningContextAssembler assembler = new PlanningContextAssembler(store);
        PlanningContext ctx = assembler.assemble("goal_1", "diag_1");

        assertThat(ctx.getTimeBudgetConstraint().getMaxTasks()).isGreaterThanOrEqualTo(4);
    }

    @Test
    void unrelatedGoal_within30Min_smallGranularity_staysAt3() {
        InMemoryStore store = new InMemoryStore();
        store.getGoals().put(
                "goal_1",
                StructuredLearningGoal.builder()
                        .rawGoalText("我想搞懂二叉树遍历")
                        .timeBudget(TimeBudget.WITHIN_30_MIN)
                        .build());
        store.getGoalContextSnapshots().put(
                "goal_1",
                GoalContextSnapshot.builder()
                        .entryGranularity(EntryGranularity.SMALL)
                        .build());

        PlanningContextAssembler assembler = new PlanningContextAssembler(store);
        PlanningContext ctx = assembler.assemble("goal_1", "diag_1");

        assertThat(ctx.getTimeBudgetConstraint().getMaxTasks()).isEqualTo(3);
    }
}
