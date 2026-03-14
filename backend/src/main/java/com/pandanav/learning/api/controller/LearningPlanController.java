package com.pandanav.learning.api.controller;

import com.pandanav.learning.api.dto.ApiEnvelope;
import com.pandanav.learning.api.dto.plan.ConfirmLearningPlanResponse;
import com.pandanav.learning.api.dto.plan.LearningPlanPreviewResponse;
import com.pandanav.learning.api.dto.plan.PreviewLearningPlanRequest;
import com.pandanav.learning.application.command.ConfirmLearningPlanCommand;
import com.pandanav.learning.application.command.PreviewLearningPlanCommand;
import com.pandanav.learning.application.service.learningplan.LearningPlanService;
import com.pandanav.learning.auth.UserContextHolder;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/learning-plans")
public class LearningPlanController {

    private final LearningPlanService learningPlanService;

    public LearningPlanController(LearningPlanService learningPlanService) {
        this.learningPlanService = learningPlanService;
    }

    @PostMapping("/preview")
    public ApiEnvelope<LearningPlanPreviewResponse> preview(@Valid @RequestBody PreviewLearningPlanRequest request) {
        LearningPlanPreviewResponse response = learningPlanService.preview(new PreviewLearningPlanCommand(
            UserContextHolder.getRequiredUserId(),
            request.goalId(),
            request.diagnosisId(),
            request.courseName(),
            request.chapterName(),
            request.goalText(),
            request.adjustments()
        ));
        return ApiEnvelope.ok(response, response.planSource());
    }

    @PostMapping("/{planId}/confirm")
    public ApiEnvelope<ConfirmLearningPlanResponse> confirm(@PathVariable @Positive Long planId) {
        return ApiEnvelope.ok(learningPlanService.confirm(new ConfirmLearningPlanCommand(
            planId,
            UserContextHolder.getRequiredUserId()
        )));
    }

    @GetMapping("/{planId}")
    public ApiEnvelope<LearningPlanPreviewResponse> get(@PathVariable @Positive Long planId) {
        return ApiEnvelope.ok(learningPlanService.get(planId, UserContextHolder.getRequiredUserId()));
    }
}
