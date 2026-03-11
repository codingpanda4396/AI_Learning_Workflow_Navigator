package com.pandanav.learning.api.controller;

import com.pandanav.learning.api.dto.feedback.LearningReportResponse;
import com.pandanav.learning.api.dto.feedback.NextStepRecommendationResponse;
import com.pandanav.learning.api.dto.session.GrowthDashboardRecentPerformanceResponse;
import com.pandanav.learning.api.dto.session.GrowthDashboardResponse;
import com.pandanav.learning.application.service.LearningInsightQueryService;
import com.pandanav.learning.auth.UserContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LearningInsightControllerTest {

    private final LearningInsightQueryService learningInsightQueryService = Mockito.mock(LearningInsightQueryService.class);

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        UserContextHolder.setUserId(10L);
        mockMvc = MockMvcBuilders.standaloneSetup(new LearningInsightController(learningInsightQueryService)).build();
    }

    @AfterEach
    void tearDown() {
        UserContextHolder.clear();
    }

    @Test
    void shouldReturnLearningReport() throws Exception {
        Mockito.when(learningInsightQueryService.getLearningReport(200L, 10L))
            .thenReturn(new LearningReportResponse(
                200L,
                300L,
                101L,
                "Binary Search",
                "TRAINING",
                "训练应用",
                78,
                new BigDecimal("75.00"),
                3,
                4,
                "summary",
                List.of("s1"),
                List.of("w1"),
                List.of("tag1"),
                null,
                List.of(),
                List.of(),
                new NextStepRecommendationResponse("REVIEW", "reason", 101L, "Binary Search", "REFLECTION", new BigDecimal("0.70")),
                true
            ));

        mockMvc.perform(get("/api/session/200/learning-feedback/report"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.session_id").value(200))
            .andExpect(jsonPath("$.next_step.recommended_action").value("REVIEW"));
    }

    @Test
    void shouldReturnGrowthDashboard() throws Exception {
        Mockito.when(learningInsightQueryService.getGrowthDashboard(200L, 10L))
            .thenReturn(new GrowthDashboardResponse(
                200L,
                "cs101",
                "ch1",
                2,
                1,
                new BigDecimal("81.00"),
                101L,
                "Binary Search",
                "TRAINING",
                "训练应用",
                List.of("Binary Search: LOW_MASTERY_SCORE"),
                new GrowthDashboardRecentPerformanceResponse(3, new BigDecimal("82.00"), 88, List.of("BOUNDARY_CASE")),
                new NextStepRecommendationResponse("ADVANCE", "reason", 102L, "Divide and Conquer", "STRUCTURE", new BigDecimal("0.88")),
                List.of()
            ));

        mockMvc.perform(get("/api/session/200/growth-dashboard"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.current_node_id").value(101))
            .andExpect(jsonPath("$.recommended_next_step.recommended_action").value("ADVANCE"));
    }
}
