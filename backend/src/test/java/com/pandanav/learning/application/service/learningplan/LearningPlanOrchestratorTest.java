package com.pandanav.learning.application.service.learningplan;

import com.pandanav.learning.domain.model.LearningPlanContextNode;
import com.pandanav.learning.domain.model.LearningPlanPlanningContext;
import com.pandanav.learning.domain.model.LlmPlanDecisionResult;
import com.pandanav.learning.domain.model.PlanAdjustments;
import com.pandanav.learning.domain.service.DefaultDecisionFactory;
import com.pandanav.learning.domain.service.LearningPlanDecisionValidator;
import com.pandanav.learning.domain.service.PlanCandidatePlanner;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LearningPlanOrchestratorTest {

    @Test
    void shouldUseLlmPlanWhenDecisionIsValid() {
        LearningPlanOrchestrator orchestrator = orchestrator(Optional.of(validDecision()));

        LearningPlanOrchestrator.OrchestratedPlan result = orchestrator.preview(sampleContext());

        assertFalse(result.fallbackApplied());
        assertEquals(PlanSource.LLM, result.planSource());
        assertEquals("LLM", result.preview().summary().contentSourceType());
        assertEquals("101", result.preview().summary().recommendedStartNodeId());
        assertNotNull(result.personalizedNarrative());
        assertTrue(containsNode(result.preview().pathPreview(), result.preview().summary().recommendedStartNodeId()));
    }

    @Test
    void shouldFallbackToRuleWhenLlmDecisionMissing() {
        LearningPlanOrchestrator orchestrator = orchestrator(Optional.empty());

        LearningPlanOrchestrator.OrchestratedPlan result = orchestrator.preview(sampleContext());

        assertEquals(PlanSource.RULE_FALLBACK, result.planSource());
        assertTrue(result.fallbackApplied());
        assertEquals("FALLBACK", result.preview().summary().contentSourceType());
        assertTrue(result.fallbackReasons().stream().anyMatch(item -> item.contains("fallback_level:L2_FULL_DEFAULT")));
        assertTrue(containsNode(result.preview().pathPreview(), result.preview().summary().recommendedStartNodeId()));
    }

    @Test
    void shouldKeepSelectedNodeInsideCurrentContext() {
        LearningPlanOrchestrator orchestrator = orchestrator(Optional.of(validDecision()));

        LearningPlanOrchestrator.OrchestratedPlan result = orchestrator.preview(sampleContext());

        List<String> contextNodeIds = sampleContext().nodes().stream().map(LearningPlanContextNode::planNodeId).toList();
        assertTrue(contextNodeIds.contains(result.preview().summary().recommendedStartNodeId()));
        assertTrue(containsNode(result.preview().pathPreview(), result.preview().summary().recommendedStartNodeId()));
    }

    private LearningPlanOrchestrator orchestrator(Optional<LlmPlanDecisionResult> llmDecisionResult) {
        LearningPlanDecisionLlmService decisionLlmService = mock(LearningPlanDecisionLlmService.class);
        when(decisionLlmService.decide(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any()))
            .thenReturn(llmDecisionResult);

        return new LearningPlanOrchestrator(
            new LearnerStateAssembler(new DefaultLearnerStateInterpreter()),
            new PlanCandidatePlanner(),
            new DefaultDecisionFactory(),
            decisionLlmService,
            new LearningPlanDecisionValidator(),
            new LearningPlanPreviewViewAssembler(),
            new DefaultLearnerStateInterpreter(),
            new LlmEnhancedPersonalizedNarrativeGenerator(new RuleBasedPersonalizedNarrativeGenerator())
        );
    }

    private LlmPlanDecisionResult validDecision() {
        return new LlmPlanDecisionResult(
            "101",
            "FOUNDATION_FIRST",
            "STANDARD",
            "先补齐链表基础可显著降低后续回退风险。",
            "当前主要卡在前置概念连接，建议先做稳健起步。",
            List.of("近期错因集中在概念边界。", "该节点位于依赖链上游。"),
            List.of(),
            List.of("先画出 linked-list basics 关系图", "围绕 linked-list basics 用正反例解释关键边界", "完成 linked-list basics 3题短练并复盘")
        );
    }

    private boolean containsNode(List<com.pandanav.learning.domain.model.PlanPathNode> path, String nodeId) {
        return path.stream().anyMatch(item -> nodeId.equals(item.nodeId()));
    }

    private LearningPlanPlanningContext sampleContext() {
        return new LearningPlanPlanningContext(
            1L,
            "goal-1",
            "diag-1",
            "course-1",
            "chapter-1",
            "master linked list basics",
            null,
            List.of(
                new LearningPlanContextNode("101", 101L, "linked-list basics", 1, 1, 40, 2, List.of("LOW_MASTERY"), List.of("CONCEPT_CONFUSION"), List.of()),
                new LearningPlanContextNode("102", 102L, "pointer movement", 2, 2, 55, 1, List.of(), List.of(), List.of("101")),
                new LearningPlanContextNode("103", 103L, "list interview drills", 3, 3, 72, 1, List.of(), List.of(), List.of("102"))
            ),
            List.of("CONCEPT_CONFUSION"),
            List.of(55, 68),
            List.of("linked-list basics"),
            "Current weak point is linked-list basics",
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
    }
}
