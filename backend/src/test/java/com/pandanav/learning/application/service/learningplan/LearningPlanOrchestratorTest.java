package com.pandanav.learning.application.service.learningplan;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pandanav.learning.application.service.llm.LlmJsonParser;
import com.pandanav.learning.domain.llm.LlmGateway;
import com.pandanav.learning.domain.llm.model.LlmInvocationProfile;
import com.pandanav.learning.domain.llm.model.LlmStage;
import com.pandanav.learning.domain.llm.model.LlmTextResult;
import com.pandanav.learning.domain.llm.model.LlmUsage;
import com.pandanav.learning.domain.model.LearningPlanContextNode;
import com.pandanav.learning.domain.model.LearningPlanPlanningContext;
import com.pandanav.learning.domain.model.PlanAdjustments;
import com.pandanav.learning.domain.service.LearningPlanPromptBuilder;
import com.pandanav.learning.domain.service.LearningPlanResultValidator;
import com.pandanav.learning.domain.service.RuleBasedPlanBuilder;
import com.pandanav.learning.infrastructure.config.LlmProperties;
import com.pandanav.learning.infrastructure.observability.LlmCallLogger;
import com.pandanav.learning.infrastructure.observability.LlmFailureClassifier;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LearningPlanOrchestratorTest {

    @Test
    void shouldUseLlmPlanWhenJsonIsValid() {
        LearningPlanOrchestrator orchestrator = orchestrator(llmResult(validJson()));

        LearningPlanOrchestrator.OrchestratedPlan result = orchestrator.preview(sampleContext());

        assertFalse(result.fallbackApplied());
        assertEquals(PlanSource.LLM, result.planSource());
        assertEquals("provider:model", result.llmTraceId());
        assertEquals("LLM", result.preview().summary().contentSourceType());
    }

    @Test
    void shouldUseLlmPlanWhenDirtyTextContainsExtractableJson() {
        LearningPlanOrchestrator orchestrator = orchestrator(llmResult("Plan preview:\n```json\n" + validJson() + "\n```"));

        LearningPlanOrchestrator.OrchestratedPlan result = orchestrator.preview(sampleContext());

        assertFalse(result.fallbackApplied());
        assertEquals(PlanSource.LLM, result.planSource());
    }

    @Test
    void shouldUseLlmPlanWhenTaskPreviewIsPartialButRecoverable() {
        LearningPlanOrchestrator orchestrator = orchestrator(llmResult(partialJson()));

        LearningPlanOrchestrator.OrchestratedPlan result = orchestrator.preview(sampleContext());

        assertFalse(result.fallbackApplied());
        assertEquals(PlanSource.LLM, result.planSource());
        assertEquals(4, result.preview().taskPreview().size());
        assertEquals(List.of("STRUCTURE", "UNDERSTANDING", "TRAINING", "REFLECTION"),
            result.preview().taskPreview().stream().map(task -> task.stage()).toList());
    }

    @Test
    void shouldFallbackWhenJsonIsUnrecoverable() {
        LearningPlanOrchestrator orchestrator = orchestrator(llmResult("```json\n{\"headline\":\"broken\"\n```"));

        LearningPlanOrchestrator.OrchestratedPlan result = orchestrator.preview(sampleContext());

        assertTrue(result.fallbackApplied());
        assertEquals(PlanSource.RULE_FALLBACK, result.planSource());
        assertEquals(List.of("JSON_EXTRA_TEXT"), result.fallbackReasons());
        assertTrue(result.personalizedNarrative() != null);
        assertEquals(NarrativeSource.FALLBACK, result.narrativeSource());
    }

    @Test
    void shouldFallbackWhenOutputIsTruncatedBeforeParsing() {
        LearningPlanOrchestrator orchestrator = orchestrator(llmResult(
            "{\"headline\":\"truncated",
            new LlmUsage(587, 320, 907, -1, 6103, "length", false, true)
        ));

        LearningPlanOrchestrator.OrchestratedPlan result = orchestrator.preview(sampleContext());

        assertTrue(result.fallbackApplied());
        assertEquals(PlanSource.RULE_FALLBACK, result.planSource());
        assertEquals(List.of("OUTPUT_TRUNCATED"), result.fallbackReasons());
    }

    @Test
    void shouldFallbackWhenJsonSchemaMismatches() {
        LearningPlanOrchestrator orchestrator = orchestrator(llmResult("""
            {
              "headline": "This headline is long enough",
              "decision_reasons": [{"type":"WEAKNESS_MATCH","title":"good"}],
              "focuses": ["focus-1", "focus-2"],
              "benefits": ["benefit-1", "benefit-2"],
              "next_unlocks": ["unlock-1"],
              "alternatives": [
                {"strategy":"FAST_TRACK","label":"Fast","description":"desc","tradeoff":"tradeoff"},
                {"strategy":"FOUNDATION_FIRST","label":"Foundation","description":"desc","tradeoff":"tradeoff"},
                {"strategy":"PRACTICE_FIRST","label":"Practice","description":"desc","tradeoff":"tradeoff"},
                {"strategy":"COMPRESSED_10_MIN","label":"Compressed","description":"desc","tradeoff":"tradeoff"}
              ],
              "task_preview": [
                {"stage":"STRUCTURE","title":"t1","goal":"g1","learner_action":"learner-1","ai_support":"support-1","estimated_minutes":8},
                {"stage":"UNDERSTANDING","title":"t2","goal":"g2","learner_action":"learner-2","ai_support":"support-2","estimated_minutes":8},
                {"stage":"TRAINING","title":"t3","goal":"g3","learner_action":"learner-3","ai_support":"support-3","estimated_minutes":8},
                {"stage":"REFLECTION","title":"t4","goal":"g4","learner_action":"learner-4","ai_support":"support-4","estimated_minutes":8}
              ]
            }
            """));

        LearningPlanOrchestrator.OrchestratedPlan result = orchestrator.preview(sampleContext());

        assertTrue(result.fallbackApplied());
        assertEquals(List.of("JSON_SCHEMA_MISMATCH"), result.fallbackReasons());
    }

    private LearningPlanOrchestrator orchestrator(LlmTextResult llmTextResult) {
        LlmGateway llmGateway = mock(LlmGateway.class);
        when(llmGateway.generate(eq(LlmStage.LEARNING_PLAN), any())).thenReturn(llmTextResult);

        return new LearningPlanOrchestrator(
            new RuleBasedPlanBuilder(),
            new LearningPlanPromptBuilder(),
            new LearningPlanResultValidator(),
            llmGateway,
            new LlmJsonParser(new ObjectMapper()),
            readyProperties(),
            new LlmCallLogger(mock(ObjectProvider.class)),
            new LlmFailureClassifier(),
            new DefaultLearnerStateInterpreter(),
            new RuleBasedPersonalizedNarrativeGenerator(),
            new LlmEnhancedPersonalizedNarrativeGenerator(new RuleBasedPersonalizedNarrativeGenerator())
        );
    }

    private LlmTextResult llmResult(String text) {
        return llmResult(text, null);
    }

    private LlmTextResult llmResult(String text, LlmUsage usage) {
        return new LlmTextResult(
            text,
            "provider",
            "model",
            LlmInvocationProfile.HEAVY_REASONING_TASK,
            usage,
            null,
            null
        );
    }

    private String validJson() {
        return """
            {
              "headline": "First strengthen foundations before advancing",
              "subtitle": "Start from the most blocking weak point.",
              "why_now": "This prerequisite is still unstable and affects later tasks.",
              "confidence": "HIGH",
              "current_focus_label": "tree basics",
              "current_task": {"task_title":"Map the node structure","estimated_minutes":8,"priority":"HIGH"},
              "decision_reasons": [
                {"type":"WEAKNESS_MATCH","title":"Start from the basics","description":"Current weak points show the learner still needs stable understanding of the prerequisite node."},
                {"type":"DEPENDENCY","title":"Dependency first","description":"Later traversal tasks depend on this prerequisite remaining stable."},
                {"type":"EFFICIENCY","title":"Best leverage first","description":"Fixing this node first improves the entire next stretch of the plan."}
              ],
              "alternatives": [
                {"strategy":"FAST_TRACK","label":"Fast track","description":"Move quickly to later practice.","tradeoff":"May cause repeated confusion later."},
                {"strategy":"FOUNDATION_FIRST","label":"Foundation first","description":"Strengthen prerequisite understanding first.","tradeoff":"Slower at the beginning."},
                {"strategy":"PRACTICE_FIRST","label":"Practice first","description":"Expose gaps through exercises first.","tradeoff":"Can feel harder if concepts are shaky."},
                {"strategy":"COMPRESSED_10_MIN","label":"10 minute version","description":"Shrink the current step to the minimum action.","tradeoff":"Needs follow-up reinforcement."}
              ],
              "focuses": ["solidify tree basics", "connect traversal to prior concepts"],
              "benefits": ["Reduce repeated backtracking", "Unlock later traversal practice"],
              "next_unlocks": ["binary tree traversal", "later training"],
              "next_step_label": "Move into guided traversal understanding",
              "task_preview": [
                {"stage":"STRUCTURE","title":"Map the node structure","goal":"Understand the core node relationships","learner_action":"Draw the node relationships clearly","ai_support":"Check the structure map and point out gaps","estimated_minutes":8},
                {"stage":"UNDERSTANDING","title":"Explain traversal logic","goal":"Explain why traversal works","learner_action":"Explain the traversal logic in your own words","ai_support":"Challenge each explanation with targeted why questions","estimated_minutes":8},
                {"stage":"TRAINING","title":"Practice traversal steps","goal":"Apply traversal steps accurately","learner_action":"Solve two traversal exercises step by step","ai_support":"Review each step and correct mistakes immediately","estimated_minutes":8},
                {"stage":"REFLECTION","title":"Summarize mistakes","goal":"Capture the next improvement focus","learner_action":"Write down the main mistake patterns and fixes","ai_support":"Summarize the mistake patterns and suggest next drills","estimated_minutes":8}
              ]
            }
            """;
    }

    private String partialJson() {
        return """
            {
              "headline": "First strengthen foundations before advancing",
              "subtitle": "Start from the most blocking weak point.",
              "why_now": "This prerequisite is still unstable and affects later tasks.",
              "confidence": "HIGH",
              "current_focus_label": "tree basics",
              "current_task": {"task_title":"Map the node structure","estimated_minutes":8,"priority":"HIGH"},
              "decision_reasons": [
                {"type":"WEAKNESS_MATCH","title":"Start from the basics","description":"Current weak points show the learner still needs stable understanding of the prerequisite node."},
                {"type":"DEPENDENCY","title":"Dependency first","description":"Later traversal tasks depend on this prerequisite remaining stable."},
                {"type":"EFFICIENCY","title":"Best leverage first","description":"Fixing this node first improves the entire next stretch of the plan."}
              ],
              "alternatives": [
                {"strategy":"FAST_TRACK","label":"Fast track","description":"Move quickly to later practice.","tradeoff":"May cause repeated confusion later."},
                {"strategy":"FOUNDATION_FIRST","label":"Foundation first","description":"Strengthen prerequisite understanding first.","tradeoff":"Slower at the beginning."},
                {"strategy":"PRACTICE_FIRST","label":"Practice first","description":"Expose gaps through exercises first.","tradeoff":"Can feel harder if concepts are shaky."},
                {"strategy":"COMPRESSED_10_MIN","label":"10 minute version","description":"Shrink the current step to the minimum action.","tradeoff":"Needs follow-up reinforcement."}
              ],
              "focuses": ["solidify tree basics", "connect traversal to prior concepts"],
              "benefits": ["Reduce repeated backtracking", "Unlock later traversal practice"],
              "next_unlocks": ["binary tree traversal", "later training"],
              "next_step_label": "Move into guided traversal understanding",
              "task_preview": [
                {"stage":"STRUCTURE","title":"Map the node structure","goal":"Understand the core node relationships","learner_action":"Draw the node relationships clearly","ai_support":"Check the structure map and point out gaps","estimated_minutes":8},
                {"stage":"TRAINING","title":"Practice traversal steps","goal":"Apply traversal steps accurately","learner_action":"Solve two traversal exercises step by step","ai_support":"Review each step and correct mistakes immediately","estimated_minutes":8}
              ]
            }
            """;
    }

    private LearningPlanPlanningContext sampleContext() {
        return new LearningPlanPlanningContext(
            1L,
            "goal-1",
            "diag-1",
            "course-1",
            "chapter-1",
            "master binary tree basics",
            null,
            List.of(
                new LearningPlanContextNode("101", 101L, "tree basics", 1, 1, 40, 2, List.of("LOW_MASTERY"), List.of("CONCEPT_CONFUSION"), List.of()),
                new LearningPlanContextNode("102", 102L, "binary tree traversal", 2, 2, 55, 1, List.of(), List.of(), List.of("101")),
                new LearningPlanContextNode("103", 103L, "integrated practice", 3, 3, 72, 1, List.of(), List.of(), List.of("102"))
            ),
            List.of("CONCEPT_CONFUSION"),
            List.of(55, 68),
            List.of("tree basics"),
            "Current weak point is tree basics",
            PlanAdjustments.defaults(),
            null,
            null,
            null,
            null,
            null,
            null,
            null
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
