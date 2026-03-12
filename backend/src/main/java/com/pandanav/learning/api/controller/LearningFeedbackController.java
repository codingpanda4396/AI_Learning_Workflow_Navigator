package com.pandanav.learning.api.controller;

import com.pandanav.learning.api.dto.feedback.WeakPointDiagnosisResponse;
import com.pandanav.learning.api.dto.feedback.WeakPointNodeResponse;
import com.pandanav.learning.application.service.WeakPointDiagnosisService;
import com.pandanav.learning.auth.UserContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping({"/api/sessions/{sessionId}/learning-feedback", "/api/session/{sessionId}/learning-feedback"})
public class LearningFeedbackController {

    private final WeakPointDiagnosisService weakPointDiagnosisService;

    public LearningFeedbackController(WeakPointDiagnosisService weakPointDiagnosisService) {
        this.weakPointDiagnosisService = weakPointDiagnosisService;
    }

    @Operation(summary = "Get weak-point diagnosis for current session")
    @GetMapping("/weak-points")
    public WeakPointDiagnosisResponse weakPoints(@PathVariable @Positive Long sessionId) {
        Long userId = UserContextHolder.getRequiredUserId();
        WeakPointDiagnosisService.WeakPointDiagnosisResult result = weakPointDiagnosisService.diagnoseWeakPoints(sessionId, userId);
        List<WeakPointNodeResponse> weakNodes = result.weakNodes().stream()
            .map(node -> new WeakPointNodeResponse(
                node.nodeId(),
                node.nodeName(),
                node.masteryScore(),
                node.trainingAccuracy(),
                node.latestEvaluationScore(),
                node.attemptCount(),
                node.recentErrorTags(),
                node.reasons()
            ))
            .toList();
        return new WeakPointDiagnosisResponse(result.sessionId(), result.diagnosisSummary(), weakNodes);
    }
}
