package com.pandanav.learning.domain.service;

import com.pandanav.learning.domain.enums.ConceptCodeGap;
import com.pandanav.learning.domain.enums.CurrentBlockType;
import com.pandanav.learning.domain.enums.EvidenceLevel;
import com.pandanav.learning.domain.enums.FoundationStatus;
import com.pandanav.learning.domain.enums.FrustrationRisk;
import com.pandanav.learning.domain.enums.GoalOrientation;
import com.pandanav.learning.domain.enums.MotivationRisk;
import com.pandanav.learning.domain.enums.PacePreference;
import com.pandanav.learning.domain.enums.PracticeReadiness;
import com.pandanav.learning.domain.enums.PreferredLearningMode;
import com.pandanav.learning.domain.model.LearnerState;
import com.pandanav.learning.domain.model.LearningPlanContextNode;
import com.pandanav.learning.domain.model.LearningPlanPlanningContext;
import com.pandanav.learning.domain.model.LlmPlanDecisionResult;
import com.pandanav.learning.domain.model.PlanAdjustments;
import com.pandanav.learning.domain.model.PlanCandidateSet;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlanCandidatePlannerTest {

    private final PlanCandidatePlanner planner = new PlanCandidatePlanner();
    private final DefaultDecisionFactory defaultDecisionFactory = new DefaultDecisionFactory();

    @Test
    void shouldBuildCandidateSetAndDefaultDecisionWithoutLlm() {
        LearningPlanPlanningContext context = new LearningPlanPlanningContext(
            1L,
            "goal-1",
            "diag-1",
            "course-1",
            "chapter-1",
            "掌握链表",
            null,
            List.of(
                new LearningPlanContextNode("n1", 101L, "链表基础", 1, 1, 40, 3, List.of("PREREQUISITE_GAP"), List.of("CONCEPT_CONFUSION"), List.of()),
                new LearningPlanContextNode("n2", 102L, "双指针技巧", 2, 2, 58, 2, List.of("LOW_MASTERY"), List.of("WRONG_POINTER"), List.of("n1")),
                new LearningPlanContextNode("n3", 103L, "链表面试题", 3, 3, 66, 1, List.of(), List.of(), List.of("n2"))
            ),
            List.of("CONCEPT_CONFUSION", "WRONG_POINTER"),
            List.of(52, 61),
            List.of("链表基础"),
            "基础薄弱，应用不稳",
            PlanAdjustments.defaults(),
            null,
            null,
            null,
            null,
            null,
            null,
            null,
            null
        );
        LearnerState learnerState = new LearnerState(
            GoalOrientation.UNDERSTAND_PRINCIPLE,
            PreferredLearningMode.LEARN_THEN_PRACTICE,
            PacePreference.NORMAL,
            CurrentBlockType.FOUNDATION_GAP,
            EvidenceLevel.MEDIUM,
            MotivationRisk.MEDIUM,
            FoundationStatus.WEAK,
            PracticeReadiness.NEEDS_WARMUP,
            ConceptCodeGap.MEDIUM,
            FrustrationRisk.MEDIUM,
            "已有部分证据。",
            "当前基础不稳。",
            null,
            List.of("近期有练习与错因记录。")
        );

        PlanCandidateSet candidateSet = planner.plan(context, learnerState);

        assertTrue(candidateSet.entries().size() >= 2);
        assertTrue(candidateSet.entries().size() <= 5);
        assertEquals(4, candidateSet.actionTemplates().size());
        assertFalse(candidateSet.strategies().isEmpty());
        assertFalse(candidateSet.intensities().isEmpty());
        List<String> contextNodeIds = context.nodes().stream().map(LearningPlanContextNode::planNodeId).toList();
        assertTrue(candidateSet.entries().stream().allMatch(item -> contextNodeIds.contains(item.conceptId())));

        LlmPlanDecisionResult decision = defaultDecisionFactory.create(learnerState, candidateSet);
        assertNotNull(decision.selectedConceptId());
        assertNotNull(decision.selectedStrategyCode());
        assertNotNull(decision.selectedIntensityCode());
        assertEquals(3, decision.nextActions().size());
    }
}
