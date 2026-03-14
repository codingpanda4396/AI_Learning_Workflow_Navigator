package com.pandanav.learning.application.service.learningplan;

import com.pandanav.learning.domain.enums.CurrentBlockType;
import com.pandanav.learning.domain.enums.EvidenceLevel;
import com.pandanav.learning.domain.model.LearningPlanContextNode;
import com.pandanav.learning.domain.model.LearningPlanPlanningContext;
import com.pandanav.learning.domain.model.PlanAdjustments;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DefaultLearnerStateInterpreterTest {

    private final DefaultLearnerStateInterpreter interpreter = new DefaultLearnerStateInterpreter();

    @Test
    void shouldMarkEvidenceLowWhenHistoricalDataIsMissing() {
        LearningPlanPlanningContext context = new LearningPlanPlanningContext(
            1L,
            "goal-1",
            "diag-1",
            "course-1",
            "chapter-1",
            "快速上手二叉树",
            null,
            List.of(new LearningPlanContextNode("n1", 1L, "基础框架", 1, 1, 55, 0, List.of(), List.of(), List.of())),
            List.of(),
            List.of(),
            List.of(),
            "证据较少",
            PlanAdjustments.defaults(),
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );

        assertEquals(EvidenceLevel.LOW, interpreter.interpret(context).evidenceLevel());
        assertEquals(CurrentBlockType.EVIDENCE_LOW, interpreter.interpret(context).currentBlockType());
    }

    @Test
    void shouldDetectFoundationGap() {
        LearningPlanPlanningContext context = new LearningPlanPlanningContext(
            1L,
            "goal-1",
            "diag-1",
            "course-1",
            "chapter-1",
            "掌握树结构原理",
            null,
            List.of(
                new LearningPlanContextNode("n1", 1L, "基础节点", 1, 1, 40, 3, List.of("PREREQUISITE_GAP"), List.of("CONCEPT_CONFUSION"), List.of()),
                new LearningPlanContextNode("n2", 2L, "遍历", 2, 2, 58, 2, List.of(), List.of(), List.of("n1"))
            ),
            List.of("CONCEPT_CONFUSION"),
            List.of(52, 57),
            List.of("基础节点"),
            "存在基础薄弱点",
            PlanAdjustments.defaults(),
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );

        assertEquals(CurrentBlockType.FOUNDATION_GAP, interpreter.interpret(context).currentBlockType());
    }

    @Test
    void shouldDetectApplicationGap() {
        LearningPlanPlanningContext context = new LearningPlanPlanningContext(
            1L,
            "goal-1",
            "diag-1",
            "course-1",
            "chapter-1",
            "通过练习提升应用能力",
            null,
            List.of(
                new LearningPlanContextNode("n1", 1L, "概念理解", 1, 2, 72, 3, List.of(), List.of("APPLICATION_ERROR"), List.of()),
                new LearningPlanContextNode("n2", 2L, "综合题", 2, 3, 68, 3, List.of(), List.of("PRACTICE_GAP"), List.of("n1"))
            ),
            List.of("APPLICATION_ERROR", "PRACTICE_GAP"),
            List.of(48, 55, 58),
            List.of("综合题"),
            "应用表现不稳定",
            PlanAdjustments.defaults(),
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );

        assertEquals(CurrentBlockType.APPLICATION_GAP, interpreter.interpret(context).currentBlockType());
    }
}
