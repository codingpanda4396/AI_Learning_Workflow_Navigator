package com.pandanav.learning.application.service.learningplan;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pandanav.learning.application.service.llm.LlmJsonParser;
import com.pandanav.learning.domain.llm.LlmGateway;
import com.pandanav.learning.domain.llm.model.LlmInvocationProfile;
import com.pandanav.learning.domain.llm.model.LlmTextResult;
import com.pandanav.learning.domain.model.LearningPlanContextNode;
import com.pandanav.learning.domain.model.LearningPlanPlanningContext;
import com.pandanav.learning.domain.model.PlanAdjustments;
import com.pandanav.learning.domain.service.LearningPlanPromptBuilder;
import com.pandanav.learning.domain.service.LearningPlanResultValidator;
import com.pandanav.learning.domain.service.RuleBasedPlanBuilder;
import com.pandanav.learning.infrastructure.config.LlmProperties;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LearningPlanOrchestratorTest {

    @Test
    void shouldFallbackWhenLlmReturnsInvalidStage() {
        LlmGateway llmGateway = mock(LlmGateway.class);
        when(llmGateway.generate(any())).thenReturn(new LlmTextResult(
            """
            {
              "headline": "先补前置，再推进主节点",
              "reasons": [{"type":"START_POINT","title":"从基础开始","description":"这段话足够长，可以通过长度校验。"}],
              "focuses": ["focus-1", "focus-2"],
              "task_preview": [
                {"stage":"STRUCTURE","title":"t1","goal":"g1","learner_action":"learner-1","ai_support":"support-1","estimated_minutes":8},
                {"stage":"UNDERSTANDING","title":"t2","goal":"g2","learner_action":"learner-2","ai_support":"support-2","estimated_minutes":8},
                {"stage":"TRAINING","title":"t3","goal":"g3","learner_action":"learner-3","ai_support":"support-3","estimated_minutes":8},
                {"stage":"UNKNOWN","title":"t4","goal":"g4","learner_action":"learner-4","ai_support":"support-4","estimated_minutes":8}
              ]
            }
            """,
            "provider",
            "model",
            LlmInvocationProfile.HEAVY_REASONING_TASK,
            null,
            null,
            null
        ));

        LearningPlanOrchestrator orchestrator = new LearningPlanOrchestrator(
            new RuleBasedPlanBuilder(),
            new LearningPlanPromptBuilder(),
            new LearningPlanResultValidator(),
            llmGateway,
            new LlmJsonParser(new ObjectMapper()),
            readyProperties()
        );

        LearningPlanOrchestrator.OrchestratedPlan result = orchestrator.preview(sampleContext());

        assertTrue(result.fallbackApplied());
        assertFalse(result.preview().reasons().isEmpty());
    }

    private LearningPlanPlanningContext sampleContext() {
        return new LearningPlanPlanningContext(
            1L,
            "goal-1",
            "diag-1",
            "course-1",
            "chapter-1",
            "掌握二叉树",
            null,
            List.of(
                new LearningPlanContextNode("101", 101L, "树的基础", 1, 1, 40, 2, List.of("LOW_MASTERY"), List.of("CONCEPT_CONFUSION"), List.of()),
                new LearningPlanContextNode("102", 102L, "二叉树遍历", 2, 2, 55, 1, List.of(), List.of(), List.of("101")),
                new LearningPlanContextNode("103", 103L, "综合练习", 3, 3, 72, 1, List.of(), List.of(), List.of("102"))
            ),
            List.of("CONCEPT_CONFUSION"),
            List.of(55, 68),
            List.of("树的基础"),
            "薄弱点集中在树的基础",
            PlanAdjustments.defaults()
        );
    }

    private LlmProperties readyProperties() {
        LlmProperties properties = new LlmProperties();
        properties.setEnabled(true);
        properties.setBaseUrl("http://localhost");
        properties.setApiKey("test");
        properties.setModel("model");
        return properties;
    }
}
