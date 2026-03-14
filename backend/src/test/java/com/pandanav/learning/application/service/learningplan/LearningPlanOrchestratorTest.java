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
import com.pandanav.learning.infrastructure.exception.AiGenerationException;
import com.pandanav.learning.infrastructure.observability.LlmCallLogger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    void shouldFailWhenTaskPreviewIsPartial() {
        LearningPlanOrchestrator orchestrator = orchestrator(llmResult(partialJson()));

        AiGenerationException ex = assertThrows(AiGenerationException.class, () -> orchestrator.preview(sampleContext()));
        assertEquals("PLAN_PREVIEW", ex.getStage());
        assertEquals("JSON_SCHEMA_MISMATCH", ex.getReason());
    }

    @Test
    void shouldFailWhenJsonIsUnrecoverable() {
        LearningPlanOrchestrator orchestrator = orchestrator(llmResult("```json\n{\"headline\":\"broken\"\n```"));

        AiGenerationException ex = assertThrows(AiGenerationException.class, () -> orchestrator.preview(sampleContext()));
        assertEquals("PLAN_PREVIEW", ex.getStage());
        assertEquals("JSON_PARSE_FAILED", ex.getReason());
    }

    @Test
    void shouldFailWhenOutputIsTruncatedBeforeParsing() {
        LearningPlanOrchestrator orchestrator = orchestrator(llmResult(
            "{\"headline\":\"truncated",
            new LlmUsage(587, 320, 907, -1, 6103, "length", false, true)
        ));

        AiGenerationException ex = assertThrows(AiGenerationException.class, () -> orchestrator.preview(sampleContext()));
        assertEquals("PLAN_PREVIEW", ex.getStage());
        assertEquals("OUTPUT_TRUNCATED", ex.getReason());
    }

    @Test
    void shouldFailWhenJsonSchemaMismatches() {
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

        AiGenerationException ex = assertThrows(AiGenerationException.class, () -> orchestrator.preview(sampleContext()));
        assertEquals("PLAN_PREVIEW", ex.getStage());
        assertEquals("JSON_SCHEMA_MISMATCH", ex.getReason());
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
            new DefaultLearnerStateInterpreter(),
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
              "strategy_comparison": {
                "current_recommended_strategy":"FOUNDATION_FIRST",
                "options":[
                  {"strategy":"FOUNDATION_FIRST","label":"Foundation first","suitable_for":"Need stable fundamentals first","not_ideal_when":"Need immediate speed-only sprint","switching_cost_risk":"Early progress feels slower but reduces later rollback"},
                  {"strategy":"FAST_TRACK","label":"Fast track","suitable_for":"Already stable prerequisite mastery","not_ideal_when":"Current dependency is still unstable","switching_cost_risk":"Higher chance of repeated fallback"},
                  {"strategy":"PRACTICE_FIRST","label":"Practice first","suitable_for":"Need to quickly expose blind spots","not_ideal_when":"Conceptual links are still fragmented","switching_cost_risk":"Can increase frustration if errors pile up"},
                  {"strategy":"COMPRESSED_10_MIN","label":"10 minute version","suitable_for":"Only fragmented time is available","not_ideal_when":"Need deep uninterrupted understanding","switching_cost_risk":"Requires explicit follow-up reinforcement"}
                ]
              },
              "plan_guidance": {
                "why_chosen":"The recommendation prioritizes stabilizing the current dependency so later practice can stay continuous.",
                "why_not_alternatives":"Other options are useful in the right moment, but now they increase rollback risk because the dependency is still fragile.",
                "learner_mirror":"You are currently in a stage of consolidating conceptual links, not just polishing speed.",
                "first_action":"Open the first node summary and draw a two-level concept relationship map in your own words.",
                "first_checkpoint":"You can explain the dependency chain without pausing at key transitions and pass one micro-check.",
                "plan_tradeoff":"This choice is steadier but gives slower short-term wins; the payoff is less repeated rework later.",
                "if_perform_well":"The system will increase practice density and unlock the next node earlier in the next round.",
                "if_still_struggle":"The system will narrow scope, add one extra explanation pass, and reduce jump distance.",
                "if_no_time":"Switch to the compressed 10-minute version and keep one explicit follow-up task for the next session.",
                "start_prompt":"先做第一轮启动动作，马上进入状态。",
                "kickoff_steps":[
                  "写下当前节点和后续节点的依赖关系",
                  "口述一遍为什么必须先补这个节点",
                  "完成1道微型校验题并记录卡点"
                ],
                "warmup_goal":"先把依赖链条讲顺，不追求一步到位。",
                "validation_focus":"系统重点看你是否进入稳定理解节奏而非只看对错。",
                "evidence_mode":"当前证据偏少，先用低风险起步模式收集有效行为信号。",
                "adaptation_policy":"每轮根据你的实际表现提速、维持或回补，不会长期固定在同一强度。",
                "confidence_explanation":"LOW 代表证据不足下的稳健起步，不代表系统无效；本轮后会快速重估。"
              },
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
              "strategy_comparison": {
                "current_recommended_strategy":"FOUNDATION_FIRST",
                "options":[
                  {"strategy":"FOUNDATION_FIRST","label":"Foundation first","suitable_for":"Need stable fundamentals first","not_ideal_when":"Need immediate speed-only sprint","switching_cost_risk":"Early progress feels slower but reduces later rollback"},
                  {"strategy":"FAST_TRACK","label":"Fast track","suitable_for":"Already stable prerequisite mastery","not_ideal_when":"Current dependency is still unstable","switching_cost_risk":"Higher chance of repeated fallback"},
                  {"strategy":"PRACTICE_FIRST","label":"Practice first","suitable_for":"Need to quickly expose blind spots","not_ideal_when":"Conceptual links are still fragmented","switching_cost_risk":"Can increase frustration if errors pile up"},
                  {"strategy":"COMPRESSED_10_MIN","label":"10 minute version","suitable_for":"Only fragmented time is available","not_ideal_when":"Need deep uninterrupted understanding","switching_cost_risk":"Requires explicit follow-up reinforcement"}
                ]
              },
              "plan_guidance": {
                "why_chosen":"The recommendation prioritizes stabilizing the current dependency so later practice can stay continuous.",
                "why_not_alternatives":"Other options are useful in the right moment, but now they increase rollback risk because the dependency is still fragile.",
                "learner_mirror":"You are currently in a stage of consolidating conceptual links, not just polishing speed.",
                "first_action":"Open the first node summary and draw a two-level concept relationship map in your own words.",
                "first_checkpoint":"You can explain the dependency chain without pausing at key transitions and pass one micro-check.",
                "plan_tradeoff":"This choice is steadier but gives slower short-term wins; the payoff is less repeated rework later.",
                "if_perform_well":"The system will increase practice density and unlock the next node earlier in the next round.",
                "if_still_struggle":"The system will narrow scope, add one extra explanation pass, and reduce jump distance.",
                "if_no_time":"Switch to the compressed 10-minute version and keep one explicit follow-up task for the next session.",
                "start_prompt":"先做第一轮启动动作，马上进入状态。",
                "kickoff_steps":[
                  "写下当前节点和后续节点的依赖关系",
                  "口述一遍为什么必须先补这个节点",
                  "完成1道微型校验题并记录卡点"
                ],
                "warmup_goal":"先把依赖链条讲顺，不追求一步到位。",
                "validation_focus":"系统重点看你是否进入稳定理解节奏而非只看对错。",
                "evidence_mode":"当前证据偏少，先用低风险起步模式收集有效行为信号。",
                "adaptation_policy":"每轮根据你的实际表现提速、维持或回补，不会长期固定在同一强度。",
                "confidence_explanation":"LOW 代表证据不足下的稳健起步，不代表系统无效；本轮后会快速重估。"
              },
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
