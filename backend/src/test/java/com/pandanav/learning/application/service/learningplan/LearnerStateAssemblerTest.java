package com.pandanav.learning.application.service.learningplan;

import com.pandanav.learning.domain.enums.ConceptCodeGap;
import com.pandanav.learning.domain.enums.FoundationStatus;
import com.pandanav.learning.domain.enums.PracticeReadiness;
import com.pandanav.learning.domain.model.LearnerState;
import com.pandanav.learning.domain.model.LearningPlanContextNode;
import com.pandanav.learning.domain.model.LearningPlanPlanningContext;
import com.pandanav.learning.domain.model.PlanAdjustments;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class LearnerStateAssemblerTest {

    private final LearnerStateAssembler assembler = new LearnerStateAssembler(new DefaultLearnerStateInterpreter());

    @Test
    void shouldAssembleDeterministicDecisionSignalsFromContext() {
        LearningPlanPlanningContext context = new LearningPlanPlanningContext(
            1L,
            "goal-1",
            "diag-1",
            "course-1",
            "chapter-1",
            "掌握链表基础并通过练习巩固",
            null,
            List.of(
                new LearningPlanContextNode("n1", 101L, "链表基础", 1, 1, 42, 3, List.of("PREREQUISITE_GAP"), List.of("CONCEPT_CONFUSION"), List.of()),
                new LearningPlanContextNode("n2", 102L, "链表应用", 2, 2, 55, 2, List.of("LOW_MASTERY"), List.of("WRONG_POINTER"), List.of("n1"))
            ),
            List.of("CONCEPT_CONFUSION", "WRONG_POINTER"),
            List.of(46, 51, 55),
            List.of("链表基础"),
            "目前在前置概念和应用连接上都不稳定",
            PlanAdjustments.defaults(),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );

        LearnerState learnerState = assembler.assemble(context);

        assertEquals(FoundationStatus.WEAK, learnerState.foundationStatus());
        assertEquals(PracticeReadiness.NEEDS_WARMUP, learnerState.practiceReadiness());
        assertEquals(ConceptCodeGap.MEDIUM, learnerState.conceptCodeGap());
        assertFalse(learnerState.evidenceSummaries().isEmpty());
    }
}
