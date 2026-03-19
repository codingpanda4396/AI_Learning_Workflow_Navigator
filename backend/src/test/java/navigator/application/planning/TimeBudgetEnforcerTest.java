package navigator.application.planning;

import navigator.domain.enums.TaskType;
import navigator.domain.model.PlanStage;
import navigator.domain.model.TaskBlueprint;
import navigator.domain.model.TimeBudgetConstraint;
import navigator.domain.enums.TimeBudget;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TimeBudgetEnforcerTest {

    private final TimeBudgetEnforcer enforcer = new TimeBudgetEnforcer();

    @Test
    void within15Min_constrainsTo2TasksAnd15Minutes() {
        PlanTemplateFactory.StagesAndTasks original = createConceptClarificationStyle();
        TimeBudgetConstraint constraint = TimeBudgetConstraint.builder()
                .timeBudget(TimeBudget.WITHIN_15_MIN)
                .totalMinutesCap(15)
                .minTasks(1)
                .maxTasks(2)
                .build();
        PlanTemplateFactory.StagesAndTasks result = enforcer.applyConstraint(original, constraint);
        assertThat(result.tasks).hasSize(2);
        int total = result.tasks.stream().mapToInt(t -> t.getEstimatedMinutes() != null ? t.getEstimatedMinutes() : 0).sum();
        assertThat(total).isLessThanOrEqualTo(15);
    }

    @Test
    void within30Min_constrainsTo3Tasks() {
        PlanTemplateFactory.StagesAndTasks original = createConceptClarificationStyle();
        TimeBudgetConstraint constraint = TimeBudgetConstraint.builder()
                .timeBudget(TimeBudget.WITHIN_30_MIN)
                .totalMinutesCap(30)
                .minTasks(1)
                .maxTasks(3)
                .build();
        PlanTemplateFactory.StagesAndTasks result = enforcer.applyConstraint(original, constraint);
        assertThat(result.tasks).hasSize(3);
        int total = result.tasks.stream().mapToInt(t -> t.getEstimatedMinutes() != null ? t.getEstimatedMinutes() : 0).sum();
        assertThat(total).isLessThanOrEqualTo(30);
    }

    @Test
    void noConstraint_whenCapNegative_returnsOriginal() {
        PlanTemplateFactory.StagesAndTasks original = createConceptClarificationStyle();
        TimeBudgetConstraint constraint = TimeBudgetConstraint.builder()
                .timeBudget(TimeBudget.LONG_TERM)
                .totalMinutesCap(-1)
                .maxTasks(8)
                .build();
        PlanTemplateFactory.StagesAndTasks result = enforcer.applyConstraint(original, constraint);
        assertThat(result.tasks).hasSize(original.tasks.size());
    }

    @Test
    void withinBudget_returnsOriginal() {
        PlanTemplateFactory.StagesAndTasks original = createConceptClarificationStyle();
        TimeBudgetConstraint constraint = TimeBudgetConstraint.builder()
                .timeBudget(TimeBudget.WITHIN_60_MIN)
                .totalMinutesCap(60)
                .maxTasks(6)
                .build();
        PlanTemplateFactory.StagesAndTasks result = enforcer.applyConstraint(original, constraint);
        assertThat(result.tasks).hasSize(original.tasks.size());
    }

    private PlanTemplateFactory.StagesAndTasks createConceptClarificationStyle() {
        List<PlanStage> stages = List.of(
                PlanStage.builder().stageCode("STAGE_1").title("核心概念澄清").estimatedMinutes(12).build(),
                PlanStage.builder().stageCode("STAGE_2").title("最小应用").estimatedMinutes(8).build()
        );
        List<TaskBlueprint> tasks = List.of(
                task("t1", "澄清定义", TaskType.CONCEPT_EXPLAIN, 10),
                task("t2", "对照易混概念", TaskType.COMPARE_AND_CONNECT, 8),
                task("t3", "最小例子", TaskType.GUIDED_EXAMPLE, 8),
                task("t4", "自解释", TaskType.SELF_EXPLANATION, 6)
        );
        return new PlanTemplateFactory.StagesAndTasks(stages, tasks);
    }

    private TaskBlueprint task(String id, String title, TaskType type, int minutes) {
        return TaskBlueprint.builder()
                .taskId(id)
                .title(title)
                .taskType(type)
                .goal(title)
                .estimatedMinutes(minutes)
                .build();
    }
}
