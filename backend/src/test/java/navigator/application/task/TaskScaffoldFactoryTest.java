package navigator.application.task;

import navigator.domain.enums.TaskType;
import navigator.domain.model.CognitiveUnit;
import navigator.domain.model.TaskBlueprint;
import navigator.domain.model.TaskScaffold;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TaskScaffoldFactoryTest {

    @Test
    void build_populatesFourCognitiveUnitsAndTaskLevelIntent() {
        TaskBlueprint bp = TaskBlueprint.builder()
                .taskId("t1")
                .title("链表")
                .taskType(TaskType.CONCEPT_EXPLAIN)
                .goal("理解链表插入与数组的差异")
                .recommendedPromptTemplate("链表的 next 指针在插入时具体怎么改？")
                .build();

        var scaffold = TaskScaffoldFactory.build("sess-1", bp, null);

        assertNotNull(scaffold.getTaskLevelLearningIntent());
        assertTrue(scaffold.getTaskLevelLearningIntent().startsWith("这一步我们一起搞懂："));
        assertEquals(scaffold.getTaskLevelLearningIntent(), scaffold.getLearningObjective());

        List<CognitiveUnit> units = scaffold.getCognitiveUnits();
        assertNotNull(units);
        assertEquals(4, units.size());
        assertEquals("understand", units.get(0).getUnitId());
        assertEquals("explore", units.get(1).getUnitId());
        assertEquals("self_explain", units.get(2).getUnitId());
        assertEquals("verify", units.get(3).getUnitId());

        var explore = units.get(1);
        assertNotNull(explore.getPrompts());
        assertTrue(explore.getPrompts().stream().anyMatch(p -> p.isRequired()));
        assertTrue(explore.getPrompts().stream().anyMatch(p -> !p.isRequired()));
    }

    @Test
    void ensureCognitiveUnits_fillsWhenMissing() {
        TaskBlueprint bp = TaskBlueprint.builder()
                .taskId("t2")
                .title("数组")
                .taskType(TaskType.MICRO_PRACTICE)
                .goal("二分查找边界")
                .build();

        var scaffold = TaskScaffold.builder()
                .taskId("t2")
                .sessionId("s")
                .taskType("MICRO_PRACTICE")
                .learningObjective("old-plain-goal")
                .cognitiveUnits(null)
                .build();

        TaskScaffoldFactory.ensureCognitiveUnits(scaffold, bp, null);

        assertNotNull(scaffold.getCognitiveUnits());
        assertEquals(4, scaffold.getCognitiveUnits().size());
        assertTrue(scaffold.getTaskLevelLearningIntent().contains("小步练习"));
    }
}
