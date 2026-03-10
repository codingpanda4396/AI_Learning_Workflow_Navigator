package com.pandanav.learning.api.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pandanav.learning.api.dto.practice.ApplyPracticeFeedbackActionRequest;
import com.pandanav.learning.api.dto.practice.PracticeFeedbackReportResponse;
import com.pandanav.learning.api.dto.practice.PracticeItemResponse;
import com.pandanav.learning.api.dto.practice.PracticeQuizResponse;
import com.pandanav.learning.application.service.PracticeService;
import com.pandanav.learning.auth.UserContextHolder;
import com.pandanav.learning.domain.model.PracticeFeedbackReport;
import com.pandanav.learning.domain.model.PracticeItem;
import com.pandanav.learning.domain.model.PracticeQuiz;
import com.pandanav.learning.infrastructure.exception.InternalServerException;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@Validated
@RestController
@RequestMapping("/api/session/{sessionId}/tasks/{taskId}/quiz")
public class PracticeQuizController {

    private final PracticeService practiceService;
    private final ObjectMapper objectMapper;

    public PracticeQuizController(PracticeService practiceService, ObjectMapper objectMapper) {
        this.practiceService = practiceService;
        this.objectMapper = objectMapper;
    }

    @Operation(summary = "Request async quiz generation for training task")
    @PostMapping("/generate")
    public PracticeQuizResponse generate(
        @PathVariable @Positive Long sessionId,
        @PathVariable @Positive Long taskId
    ) {
        Long userId = UserContextHolder.getRequiredUserId();
        PracticeQuiz quiz = practiceService.requestQuizGeneration(sessionId, taskId, userId);
        return toQuizResponse(quiz, List.of());
    }

    @Operation(summary = "Get current quiz snapshot")
    @GetMapping
    public PracticeQuizResponse getQuiz(
        @PathVariable @Positive Long sessionId,
        @PathVariable @Positive Long taskId
    ) {
        Long userId = UserContextHolder.getRequiredUserId();
        PracticeQuiz quiz = practiceService.getQuiz(sessionId, taskId, userId);
        List<PracticeItemResponse> questions = practiceService.listPracticeItems(sessionId, taskId, userId).stream()
            .map(this::toItemResponse)
            .toList();
        return toQuizResponse(quiz, questions);
    }

    @Operation(summary = "Get generated practice feedback report")
    @GetMapping("/feedback")
    public PracticeFeedbackReportResponse getFeedback(
        @PathVariable @Positive Long sessionId,
        @PathVariable @Positive Long taskId
    ) {
        Long userId = UserContextHolder.getRequiredUserId();
        PracticeFeedbackReport report = practiceService.getFeedbackReport(sessionId, taskId, userId);
        return new PracticeFeedbackReportResponse(
            report.getId(),
            report.getQuizId(),
            report.getSessionId(),
            report.getTaskId(),
            report.getDiagnosisSummary(),
            parseArray(report.getStrengthsJson()),
            parseArray(report.getWeaknessesJson()),
            parseArray(report.getReviewFocusJson()),
            report.getNextRoundAdvice(),
            report.getRecommendedAction(),
            report.getSource()
        );
    }

    @Operation(summary = "Apply review or next round action")
    @PostMapping("/feedback/action")
    public PracticeQuizResponse applyFeedbackAction(
        @PathVariable @Positive Long sessionId,
        @PathVariable @Positive Long taskId,
        @Valid @RequestBody ApplyPracticeFeedbackActionRequest request
    ) {
        Long userId = UserContextHolder.getRequiredUserId();
        PracticeQuiz quiz = practiceService.applyFeedbackAction(sessionId, taskId, userId, request.action());
        List<PracticeItemResponse> questions = practiceService.listPracticeItems(sessionId, taskId, userId).stream()
            .map(this::toItemResponse)
            .toList();
        return toQuizResponse(quiz, questions);
    }

    private PracticeQuizResponse toQuizResponse(PracticeQuiz quiz, List<PracticeItemResponse> questions) {
        return new PracticeQuizResponse(
            quiz.getId(),
            quiz.getSessionId(),
            quiz.getTaskId(),
            quiz.getStatus().name(),
            quiz.getQuestionCount(),
            quiz.getAnsweredCount(),
            quiz.getGenerationSource(),
            quiz.getFailureReason(),
            questions
        );
    }

    private PracticeItemResponse toItemResponse(PracticeItem item) {
        return new PracticeItemResponse(
            item.getId(),
            item.getQuestionType().name(),
            item.getStem(),
            parseOptions(item.getOptionsJson()),
            item.getDifficulty(),
            item.getSource().name(),
            item.getStatus().name()
        );
    }

    private JsonNode parseOptions(String optionsJson) {
        if (optionsJson == null || optionsJson.isBlank()) {
            return objectMapper.createArrayNode();
        }
        try {
            return objectMapper.readTree(optionsJson);
        } catch (Exception ex) {
            throw new InternalServerException("Stored options_json is invalid.");
        }
    }

    private List<String> parseArray(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            JsonNode parsed = objectMapper.readTree(json);
            if (!parsed.isArray()) {
                return List.of();
            }
            List<String> values = new ArrayList<>();
            for (JsonNode node : parsed) {
                if (node.isTextual() && !node.asText().isBlank()) {
                    values.add(node.asText().trim());
                }
            }
            return values;
        } catch (Exception ex) {
            throw new InternalServerException("Stored feedback json is invalid.");
        }
    }
}
