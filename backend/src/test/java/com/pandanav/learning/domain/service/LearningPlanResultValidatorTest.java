package com.pandanav.learning.domain.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pandanav.learning.domain.llm.model.LearningPlanLlmResult;
import com.pandanav.learning.domain.model.LearningPlanPreview;
import com.pandanav.learning.domain.model.LearningPlanSummary;
import com.pandanav.learning.domain.model.PlanAdjustments;
import com.pandanav.learning.domain.model.PlanAlternative;
import com.pandanav.learning.domain.model.PlanPathNode;
import com.pandanav.learning.domain.model.PlanReason;
import com.pandanav.learning.domain.model.PlanTaskPreview;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LearningPlanResultValidatorTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final LearningPlanResultValidator validator = new LearningPlanResultValidator();

    @Test
    void shouldReportSpecificSchemaErrors() throws Exception {
        String json = """
            {
              "headline": "valid headline text",
              "decision_reasons": [{"type":"WEAKNESS_MATCH","title":"good"}],
              "focuses": ["focus-1", 2],
              "benefits": ["benefit-1", "benefit-2"],
              "next_unlocks": ["unlock-1"],
              "alternatives": [
                {"strategy":"FAST_TRACK","label":"Fast","description":"desc","tradeoff":"tradeoff"},
                {"strategy":"FOUNDATION_FIRST","label":"Foundation","description":"desc","tradeoff":"tradeoff"},
                {"strategy":"PRACTICE_FIRST","label":"Practice","description":"desc","tradeoff":"tradeoff"},
                {"strategy":"COMPRESSED_10_MIN","label":"Compressed","description":"desc","tradeoff":"tradeoff"}
              ],
              "task_preview": [{"stage":"INVALID","title":"t1","goal":"g1","learner_action":"learn more","ai_support":"coach more","estimated_minutes":"8"}]
            }
            """;

        LearningPlanSchemaValidationException ex = assertThrows(
            LearningPlanSchemaValidationException.class,
            () -> validator.parse(objectMapper.readTree(json))
        );

        assertTrue(ex.errors().contains("$.decision_reasons[0].description is required"));
        assertTrue(ex.errors().contains("$.focuses[1] must be string"));
        assertTrue(ex.errors().contains("$.task_preview[0].stage invalid enum value: INVALID"));
        assertTrue(ex.errors().contains("$.task_preview[0].estimated_minutes must be integer"));
    }

    @Test
    void shouldNormalizePartialTaskPreviewUsingFallbackStages() {
        LearningPlanLlmResult llmResult = new LearningPlanLlmResult(
            "First strengthen foundations before advancing",
            "subtitle",
            "why now",
            "HIGH",
            "tree basics",
            "Custom structure",
            8,
            "HIGH",
            List.of(new PlanReason("WEAKNESS_MATCH", "Start with basics", "The learner still needs prerequisite reinforcement before progressing.")),
            List.of("focus-1", "focus-2"),
            List.of(),
            List.of("benefit-1", "benefit-2"),
            List.of("unlock-1"),
            "next step",
            List.of(
                new PlanTaskPreview("STRUCTURE", "Custom structure", "Goal", "learner action", "ai support", 8),
                new PlanTaskPreview("TRAINING", "Custom training", "Goal", "learner action", "ai support", 9)
            )
        );

        LearningPlanLlmResult normalized = validator.normalize(llmResult, fallbackPreview());

        assertEquals(4, normalized.taskPreview().size());
        assertEquals(List.of("STRUCTURE", "UNDERSTANDING", "TRAINING", "REFLECTION"),
            normalized.taskPreview().stream().map(PlanTaskPreview::stage).toList());
        assertEquals("Custom structure", normalized.taskPreview().get(0).title());
        assertEquals("fallback understanding", normalized.taskPreview().get(1).title());
        assertEquals("Custom training", normalized.taskPreview().get(2).title());
        assertEquals("fallback reflection", normalized.taskPreview().get(3).title());
    }

    private LearningPlanPreview fallbackPreview() {
        return new LearningPlanPreview(
            new LearningPlanSummary(
                "fallback headline long enough",
                "101",
                "tree basics",
                "STANDARD",
                36,
                2,
                4,
                "subtitle",
                "why now",
                "MEDIUM",
                "tree basics",
                "fallback structure",
                6,
                "MEDIUM",
                List.of(
                    new PlanAlternative("FAST_TRACK", "Fast", "desc", "tradeoff"),
                    new PlanAlternative("FOUNDATION_FIRST", "Foundation", "desc", "tradeoff"),
                    new PlanAlternative("PRACTICE_FIRST", "Practice", "desc", "tradeoff"),
                    new PlanAlternative("COMPRESSED_10_MIN", "Compressed", "desc", "tradeoff")
                ),
                List.of("benefit-a", "benefit-b"),
                List.of("unlock-a"),
                "next step",
                "FALLBACK",
                true,
                List.of("RULE_BASED_DECISION")
            ),
            List.of(new PlanReason("WEAKNESS_MATCH", "Fallback start", "Fallback reason description is sufficiently long.")),
            List.of("focus-a", "focus-b"),
            List.of(new PlanPathNode("101", "tree basics", 1, 40, "LEARNING", true, 18, "tag")),
            List.of(
                new PlanTaskPreview("STRUCTURE", "fallback structure", "goal", "fallback learner action", "fallback ai support", 6),
                new PlanTaskPreview("UNDERSTANDING", "fallback understanding", "goal", "fallback learner action", "fallback ai support", 7),
                new PlanTaskPreview("TRAINING", "fallback training", "goal", "fallback learner action", "fallback ai support", 8),
                new PlanTaskPreview("REFLECTION", "fallback reflection", "goal", "fallback learner action", "fallback ai support", 5)
            ),
            PlanAdjustments.defaults()
        );
    }
}
