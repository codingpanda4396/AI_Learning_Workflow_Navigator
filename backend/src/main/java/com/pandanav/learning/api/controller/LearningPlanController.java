package com.pandanav.learning.api.controller;

import com.pandanav.learning.api.dto.ApiEnvelope;
import com.pandanav.learning.api.dto.plan.AdjustLearningPlanRequest;
import com.pandanav.learning.api.dto.plan.AdjustLearningPlanResponse;
import com.pandanav.learning.api.dto.plan.ConfirmLearningPlanResponse;
import com.pandanav.learning.api.dto.plan.LearningPlanPreviewResponse;
import com.pandanav.learning.api.dto.plan.PreviewLearningPlanRequest;
import com.pandanav.learning.application.command.AdjustLearningPlanCommand;
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
            request.diagnosisId(),
            request.sessionId(),
            request.courseName(),
            request.chapterName(),
            request.goalText(),
            request.adjustments()
        ));
        return ApiEnvelope.ok(response, Boolean.TRUE.equals(response.explanationGenerated()) ? "LLM_HINTS" : "TEMPLATE_ONLY");
    }

    @PostMapping("/adjust")
    public ApiEnvelope<AdjustLearningPlanResponse> adjust(@Valid @RequestBody AdjustLearningPlanRequest request) {
        AdjustLearningPlanResponse response = learningPlanService.adjust(new AdjustLearningPlanCommand(
            UserContextHolder.getRequiredUserId(),
            request.sessionId(),
            request.previewId(),
            request.strategy(),
            request.reason(),
            request.timeBudget(),
            request.userFeedback()
        ));
        return ApiEnvelope.ok(response, response.result() == null ? null : (Boolean.TRUE.equals(response.result().explanationGenerated()) ? "LLM_HINTS" : "TEMPLATE_ONLY"));
    }

    @PostMapping("/{previewId}/confirm")
    public ApiEnvelope<ConfirmLearningPlanResponse> confirm(@PathVariable @Positive Long previewId) {
        return ApiEnvelope.ok(learningPlanService.confirm(new ConfirmLearningPlanCommand(
            previewId,
            UserContextHolder.getRequiredUserId()
        )));
    }

    @GetMapping("/{previewId}")
    public ApiEnvelope<LearningPlanPreviewResponse> get(@PathVariable @Positive Long previewId) {
        return ApiEnvelope.ok(learningPlanService.get(previewId, UserContextHolder.getRequiredUserId()));
    }
}
