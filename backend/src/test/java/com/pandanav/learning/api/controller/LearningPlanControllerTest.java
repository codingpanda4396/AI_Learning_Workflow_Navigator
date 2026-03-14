package com.pandanav.learning.api.controller;

import com.pandanav.learning.api.dto.CodeLabelDto;
import com.pandanav.learning.api.dto.plan.AdjustLearningPlanResponse;
import com.pandanav.learning.api.dto.plan.ConfirmLearningPlanResponse;
import com.pandanav.learning.api.dto.plan.LearningPlanAdjustmentsDto;
import com.pandanav.learning.api.dto.plan.LearningPlanContextResponse;
import com.pandanav.learning.api.dto.plan.LearningPlanLearnerSnapshotResponse;
import com.pandanav.learning.api.dto.plan.LearningPlanMetadataResponse;
import com.pandanav.learning.api.dto.plan.LearningPlanPreviewResponse;
import com.pandanav.learning.api.dto.plan.LearningPlanRecommendationResponse;
import com.pandanav.learning.api.dto.plan.LearningPlanSummaryResponse;
import com.pandanav.learning.api.dto.plan.PlanAlternativeResponse;
import com.pandanav.learning.api.dto.plan.PlanNodeReferenceResponse;
import com.pandanav.learning.api.dto.plan.PlanPathNodeResponse;
import com.pandanav.learning.api.dto.plan.PlanPriorityNodeResponse;
import com.pandanav.learning.api.dto.plan.PlanReasonResponse;
import com.pandanav.learning.api.dto.plan.PlanTaskPreviewResponse;
import com.pandanav.learning.application.service.learningplan.LearningPlanService;
import com.pandanav.learning.auth.UserContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LearningPlanControllerTest {

    private final LearningPlanService learningPlanService = Mockito.mock(LearningPlanService.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        UserContextHolder.setUserId(1L);
        mockMvc = MockMvcBuilders.standaloneSetup(new LearningPlanController(learningPlanService))
            .setMessageConverters(new MappingJackson2HttpMessageConverter())
            .build();
    }

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    @Test
    void shouldReturnPreviewEnvelope() throws Exception {
        Mockito.when(learningPlanService.preview(Mockito.any())).thenReturn(samplePreview());

        mockMvc.perform(post("/api/learning-plans/preview")
                .contentType("application/json")
                .content("""
                    {
                      "diagnosisId":"diag-1",
                      "goalText":"master tree basics",
                      "courseName":"Data Structures",
                      "chapterName":"Trees",
                      "adjustments":{
                        "intensity":"STANDARD",
                        "learningMode":"LEARN_THEN_PRACTICE",
                        "prioritizeFoundation":true
                      }
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("OK"))
            .andExpect(jsonPath("$.data.previewId").value("101"))
            .andExpect(jsonPath("$.data.contentSource.code").value("LLM"))
            .andExpect(jsonPath("$.metadata.strategy").value("LLM"))
            .andExpect(jsonPath("$.data.recommendation.taskTitle").value("Map the structure"))
            .andExpect(jsonPath("$.data.alternatives[0].strategy").value("FAST_TRACK"))
            .andExpect(jsonPath("$.data.summary.recommendedStartNode.nodeName").value("tree basics"));
    }

    @Test
    void shouldReturnAdjustEnvelope() throws Exception {
        Mockito.when(learningPlanService.adjust(Mockito.any())).thenReturn(new AdjustLearningPlanResponse(
            samplePreview(),
            "strategy changed",
            "replanned for time limit"
        ));

        mockMvc.perform(post("/api/learning-plans/adjust")
                .contentType("application/json")
                .content("""
                    {
                      "previewId":101,
                      "strategy":"FAST_TRACK",
                      "timeBudget":10,
                      "userFeedback":"TIME_LIMITED"
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.result.previewId").value("101"))
            .andExpect(jsonPath("$.data.changeSummary").value("strategy changed"));
    }

    @Test
    void shouldReturnConfirmEnvelope() throws Exception {
        Mockito.when(learningPlanService.confirm(Mockito.any())).thenReturn(new ConfirmLearningPlanResponse("101", 500L, 101L, 800L, "/sessions/500"));

        mockMvc.perform(post("/api/learning-plans/101/confirm"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.sessionId").value(500))
            .andExpect(jsonPath("$.data.firstTaskId").value(800));
    }

    @Test
    void shouldReturnStoredPlan() throws Exception {
        Mockito.when(learningPlanService.get(101L, 1L)).thenReturn(samplePreview());

        mockMvc.perform(get("/api/learning-plans/101"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.pathPreview[0].isRecommendedStart").value(true));
    }

    private LearningPlanPreviewResponse samplePreview() {
        return new LearningPlanPreviewResponse(
            "101",
            "PREVIEW_READY",
            true,
            false,
            new CodeLabelDto("RULE_ENGINE", "Rule Engine"),
            new CodeLabelDto("LLM", "LLM"),
            "LLM",
            false,
            List.of(),
            "HIGH",
            OffsetDateTime.parse("2026-03-14T10:00:00+08:00"),
            "trace-1",
            new LearningPlanSummaryResponse(
                "Strengthen basics before advancing",
                new PlanNodeReferenceResponse("101", "101", "tree basics"),
                new CodeLabelDto("STANDARD", "Standard"),
                36,
                2,
                4
            ),
            List.of(new PlanReasonResponse("WEAKNESS_MATCH", "Start with the basics", "The learner still needs a stronger prerequisite foundation before moving ahead.")),
            List.of(new PlanReasonResponse("WEAKNESS_MATCH", "Start with the basics", "The learner still needs a stronger prerequisite foundation before moving ahead.")),
            List.of(
                new PlanAlternativeResponse("FAST_TRACK", "Fast track", "Move faster", "Higher risk"),
                new PlanAlternativeResponse("FOUNDATION_FIRST", "Foundation first", "Stay stable", "Slower"),
                new PlanAlternativeResponse("PRACTICE_FIRST", "Practice first", "Expose gaps", "Can feel harder"),
                new PlanAlternativeResponse("COMPRESSED_10_MIN", "10 minute version", "Shrink current step", "Needs follow-up")
            ),
            List.of("solidify tree basics", "connect traversal to the basics"),
            new LearningPlanRecommendationResponse("Strengthen basics before advancing", "Start from the biggest blocker first", "Map the structure", 8, "HIGH", "This prerequisite still blocks later traversal work."),
            new LearningPlanLearnerSnapshotResponse("Strengthen tree traversal basics", List.of("prerequisite gap"), 40, "Skipping this step increases later confusion.", "tree basics"),
            "Recommend strengthening the prerequisite first.",
            List.of("prerequisite gap", "recent confusion"),
            List.of(new PlanPriorityNodeResponse("101", "tree basics", "This node still blocks later traversal work.")),
            List.of(new PlanPathNodeResponse(
                new PlanNodeReferenceResponse("101", "101", "tree basics"),
                new CodeLabelDto("FOUNDATION", "Foundation"),
                40,
                new CodeLabelDto("PARTIAL", "Partial"),
                true,
                18,
                "prerequisite core"
            )),
            List.of(new PlanTaskPreviewResponse(new CodeLabelDto("STRUCTURE", "Structure"), "t1", "g1", "a1", "s1", 6)),
            List.of("Reduce backtracking", "Unlock later traversal"),
            List.of("binary tree traversal"),
            "Move into traversal understanding",
            new LearningPlanAdjustmentsDto("STANDARD", "LEARN_THEN_PRACTICE", true),
            new LearningPlanContextResponse(500L, "diag-1", "Strengthen tree traversal basics", "Data Structures", "Trees", "Diagnosis summary"),
            "Next step note",
            new LearningPlanMetadataResponse("plan-preview.v3", true, "path_preview_total", "per_path_node", "per_stage_task_template")
        );
    }
}
