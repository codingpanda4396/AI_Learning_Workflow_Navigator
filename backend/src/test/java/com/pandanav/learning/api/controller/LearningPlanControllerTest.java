package com.pandanav.learning.api.controller;

import com.pandanav.learning.api.dto.plan.AdjustLearningPlanResponse;
import com.pandanav.learning.api.dto.plan.ConfirmLearningPlanResponse;
import com.pandanav.learning.api.dto.plan.LearningPlanPreviewResponse;
import com.pandanav.learning.api.dto.plan.LearningPlanAdjustmentsDto;
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
            .andExpect(jsonPath("$.data.planId").value("101"))
            .andExpect(jsonPath("$.metadata.strategy").value("TEMPLATE_ONLY"))
            .andExpect(jsonPath("$.data.recommendedEntry.title").value("tree basics"))
            .andExpect(jsonPath("$.data.recommendedStrategy.code").value("FOUNDATION_FIRST"))
            .andExpect(jsonPath("$.data.nextActions[0]").value("建立整体框架"));
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
            .andExpect(jsonPath("$.data.result.planId").value("101"))
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
            .andExpect(jsonPath("$.data.planId").value("101"));
    }

    private LearningPlanPreviewResponse samplePreview() {
        return new LearningPlanPreviewResponse(
            "101",
            "PREVIEW_READY",
            true,
            false,
            "Strengthen tree traversal basics",
            new LearningPlanPreviewResponse.RecommendedEntryResponse("101", "tree basics", 8, "先补这一段可以减少后续卡住。"),
            new LearningPlanPreviewResponse.LearnerSnapshotResponse(
                "你当前主要卡在前置基础不稳。",
                List.of("基础薄弱点集中在树结构。", "最近练习在依赖关系上反复出错。", "当前起点直接影响后续节点。")
            ),
            new LearningPlanPreviewResponse.RecommendedStrategyResponse("FOUNDATION_FIRST", "先补基础", "先补关键薄弱点更稳妥。"),
            List.of(
                new LearningPlanPreviewResponse.AlternativeStrategyResponse("FAST_TRACK", "快速推进", "这次不优先，因为容易放大当前断层。"),
                new LearningPlanPreviewResponse.AlternativeStrategyResponse("PRACTICE_FIRST", "先练后学", "这次不优先，因为概念连接还不够稳。")
            ),
            List.of("建立整体框架", "补齐关键理解", "做针对性训练"),
            new LearningPlanAdjustmentsDto("STANDARD", "LEARN_THEN_PRACTICE", true),
            "确认后系统会创建正式计划，并带你进入第一步任务。",
            false,
            OffsetDateTime.parse("2026-03-14T10:00:00+08:00"),
            "trace-1"
        );
    }
}
