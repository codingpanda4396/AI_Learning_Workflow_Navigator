package com.pandanav.learning.api.controller;

import com.pandanav.learning.api.dto.feedback.LearningReportResponse;
import com.pandanav.learning.api.dto.session.GrowthDashboardResponse;
import com.pandanav.learning.application.service.LearningInsightQueryService;
import com.pandanav.learning.auth.UserContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping({"/api/sessions/{sessionId}", "/api/session/{sessionId}"})
public class LearningInsightController {

    private final LearningInsightQueryService learningInsightQueryService;

    public LearningInsightController(LearningInsightQueryService learningInsightQueryService) {
        this.learningInsightQueryService = learningInsightQueryService;
    }

    @Operation(summary = "Get aggregated learning report for current session")
    @GetMapping("/learning-feedback/report")
    public LearningReportResponse getLearningReport(@PathVariable @Positive Long sessionId) {
        return learningInsightQueryService.getLearningReport(sessionId, UserContextHolder.getRequiredUserId());
    }

    @Operation(summary = "Get growth dashboard for current session")
    @GetMapping("/growth-dashboard")
    public GrowthDashboardResponse getGrowthDashboard(@PathVariable @Positive Long sessionId) {
        return learningInsightQueryService.getGrowthDashboard(sessionId, UserContextHolder.getRequiredUserId());
    }
}
