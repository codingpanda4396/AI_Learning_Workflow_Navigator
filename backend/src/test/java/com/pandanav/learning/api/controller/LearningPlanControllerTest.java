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
            .andExpect(jsonPath("$.data.nextActions[0]").value("建立整体框架"))
            .andExpect(jsonPath("$.data.personalizedSummary.title").value("你已经接触过树结构，但基础还不稳定"))
            .andExpect(jsonPath("$.data.currentTaskCard.tasks[0]").value("写出树结构的核心结构定义"));
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
            "系统优先推荐这一步，因为它最能减少后续卡点。",
            List.of("基础薄弱点集中在树结构", "最近练习在依赖关系上反复出错"),
            List.of("画像显示学习偏好为「CONCEPT_FIRST」，本轮策略已按该偏好排序。"),
            "跳过会导致后续节点理解和训练同时卡住。",
            "完成后会更容易进入下一轮训练。",
            "当前推荐基于近期学习证据，可信度中等偏高。",
            List.of("OVERCONFIDENCE_RISK"),
            List.of(),
            List.of("OVERCONFIDENCE_PROFILE_CONFLICT"),
            new LearningPlanAdjustmentsDto("STANDARD", "LEARN_THEN_PRACTICE", true),
            "确认后系统会创建正式计划，并带你进入第一步任务。",
            false,
            new LearningPlanPreviewResponse.PersonalizedSummaryResponse(
                "你已经接触过树结构，但基础还不稳定",
                "考虑到你更适合先理解概念再练习，系统先安排一个短任务，帮助你快速进入状态。",
                List.of("准备考试或测验", "每周 4-6 小时", "先理解概念再练习")
            ),
            new LearningPlanPreviewResponse.CurrentTaskCardResponse(
                "理解树结构的基本结构",
                8,
                "先建立对树结构关键结构与基本连接方式的直观认识。",
                List.of("写出树结构的核心结构定义", "完成一次最小可运行示例", "用两组输入验证结果"),
                List.of("理解树结构的关键结构", "掌握最基本的实现步骤", "能独立完成基础样例")
            ),
            new LearningPlanPreviewResponse.PersonalizedReasonsResponse(
                List.of("你当前目标是准备考试或测验，先稳住树结构基础会更高效。"),
                List.of("树结构基础结构是后续插入、删除与综合应用的共同前提。")
            ),
            new LearningPlanPreviewResponse.ExplanationPanelResponse(
                List.of(
                    new LearningPlanPreviewResponse.LearnerProfileItemResponse("当前基础", "学过相关内容，但基础还不稳定"),
                    new LearningPlanPreviewResponse.LearnerProfileItemResponse("学习目标", "准备考试或测验"),
                    new LearningPlanPreviewResponse.LearnerProfileItemResponse("学习方式", "先理解概念再练习"),
                    new LearningPlanPreviewResponse.LearnerProfileItemResponse("时间节奏", "每周 4-6 小时")
                ),
                "系统先安排一个低门槛练习，帮助你快速进入状态。"
            ),
            OffsetDateTime.parse("2026-03-14T10:00:00+08:00"),
            "trace-1"
        );
    }
}
