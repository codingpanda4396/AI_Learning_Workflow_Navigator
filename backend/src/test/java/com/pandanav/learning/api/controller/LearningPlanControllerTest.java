package com.pandanav.learning.api.controller;

import com.pandanav.learning.api.dto.CodeLabelDto;
import com.pandanav.learning.api.dto.plan.ConfirmLearningPlanResponse;
import com.pandanav.learning.api.dto.plan.LearningPlanAdjustmentsDto;
import com.pandanav.learning.api.dto.plan.LearningPlanContextResponse;
import com.pandanav.learning.api.dto.plan.LearningPlanMetadataResponse;
import com.pandanav.learning.api.dto.plan.LearningPlanPreviewResponse;
import com.pandanav.learning.api.dto.plan.LearningPlanSummaryResponse;
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
                      "goalId":"goal-1",
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
            .andExpect(jsonPath("$.data.summary.recommendedStartNode.nodeName").value("tree basics"))
            .andExpect(jsonPath("$.data.whyStartHere").value("建议先补齐前置概念，避免后续路径断裂"))
            .andExpect(jsonPath("$.data.priorityNodes[0].nodeId").value("101"));
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
            new CodeLabelDto("RULE_ENGINE", "规则引擎"),
            new CodeLabelDto("LLM", "LLM"),
            false,
            List.of(),
            new LearningPlanSummaryResponse(
                "Strengthen basics before advancing",
                new PlanNodeReferenceResponse("101", "101", "tree basics"),
                new CodeLabelDto("STANDARD", "Standard"),
                36,
                2,
                4
            ),
            List.of(new PlanReasonResponse("START_POINT", "Start with the basics", "The learner still needs a stronger prerequisite foundation before moving ahead.")),
            List.of("solidify tree basics", "connect traversal to the basics"),
            "建议先补齐前置概念，避免后续路径断裂",
            List.of("前置概念掌握不足", "边界条件处理不稳定"),
            List.of(new PlanPriorityNodeResponse("101", "tree basics", "这是当前最影响后续学习推进的起点")),
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
            new LearningPlanAdjustmentsDto("STANDARD", "LEARN_THEN_PRACTICE", true),
            new LearningPlanContextResponse(500L, "diag-1", "Strengthen tree traversal basics", "Data Structures", "Trees", "Diagnosis summary"),
            "Next step note",
            new LearningPlanMetadataResponse("plan-preview.v2", true, "path_preview_total", "per_path_node", "per_stage_task_template")
        );
    }
}
