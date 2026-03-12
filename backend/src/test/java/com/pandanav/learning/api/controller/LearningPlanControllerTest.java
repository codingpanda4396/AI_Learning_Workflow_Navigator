package com.pandanav.learning.api.controller;

import com.pandanav.learning.api.dto.plan.ConfirmLearningPlanResponse;
import com.pandanav.learning.api.dto.plan.LearningPlanAdjustmentsRequest;
import com.pandanav.learning.api.dto.plan.LearningPlanPreviewResponse;
import com.pandanav.learning.api.dto.plan.LearningPlanSummaryResponse;
import com.pandanav.learning.api.dto.plan.PlanPathNodeResponse;
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
                      "adjustments":{"intensity":"STANDARD","learningMode":"LEARN_THEN_PRACTICE","preferPrerequisite":true}
                    }
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("OK"))
            .andExpect(jsonPath("$.data.planId").value("101"))
            .andExpect(jsonPath("$.data.summary.recommendedStartNodeName").value("树的基础"));
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
            new LearningPlanSummaryResponse("先补基础再推进", "101", "树的基础", "STANDARD", 36, 2, 4),
            List.of(new PlanReasonResponse("START_POINT", "从树的基础开始", "因为当前掌握度低且它是后续节点的前置。")),
            List.of("先稳住树的基础", "再进入二叉树遍历"),
            List.of(new PlanPathNodeResponse("101", "树的基础", 1, 40, "LEARNING", true, 18, "前置核心")),
            List.of(new PlanTaskPreviewResponse("STRUCTURE", "t1", "g1", "a1", "s1", 6)),
            new LearningPlanAdjustmentsRequest("STANDARD", "LEARN_THEN_PRACTICE", true)
        );
    }
}
