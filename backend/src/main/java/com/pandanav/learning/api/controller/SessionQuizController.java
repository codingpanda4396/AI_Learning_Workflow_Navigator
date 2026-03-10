package com.pandanav.learning.api.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pandanav.learning.api.dto.practice.ApplyPracticeFeedbackActionRequest;
import com.pandanav.learning.api.dto.practice.SessionFeedbackResponse;
import com.pandanav.learning.api.dto.practice.SessionQuestionResultResponse;
import com.pandanav.learning.api.dto.practice.SessionQuizQuestionResponse;
import com.pandanav.learning.api.dto.practice.SessionQuizResponse;
import com.pandanav.learning.api.dto.practice.SessionQuizStatusResponse;
import com.pandanav.learning.api.dto.practice.SessionQuizSubmitRequest;
import com.pandanav.learning.application.service.SessionQuizService;
import com.pandanav.learning.auth.UserContextHolder;
import com.pandanav.learning.domain.model.PracticeFeedbackReport;
import com.pandanav.learning.domain.model.PracticeItem;
import com.pandanav.learning.domain.model.PracticeQuiz;
import com.pandanav.learning.domain.model.Task;
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
@RequestMapping("/api/sessions/{sessionId}")
public class SessionQuizController {

    private final SessionQuizService sessionQuizService;
    private final ObjectMapper objectMapper;

    public SessionQuizController(SessionQuizService sessionQuizService, ObjectMapper objectMapper) {
        this.sessionQuizService = sessionQuizService;
        this.objectMapper = objectMapper;
    }

    @Operation(summary = "Trigger async quiz generation for the current training task in session")
    @PostMapping("/quiz/generate")
    public SessionQuizStatusResponse generate(@PathVariable @Positive Long sessionId) {
        Long userId = UserContextHolder.getRequiredUserId();
        sessionQuizService.generate(sessionId, userId);
        SessionQuizService.QuizContext context = sessionQuizService.getStatus(sessionId, userId);
        return toStatusResponse(context.task(), context.quiz());
    }

    @Operation(summary = "Query quiz generation status for session")
    @GetMapping("/quiz/status")
    public SessionQuizStatusResponse getStatus(@PathVariable @Positive Long sessionId) {
        Long userId = UserContextHolder.getRequiredUserId();
        SessionQuizService.QuizContext context = sessionQuizService.getStatus(sessionId, userId);
        return toStatusResponse(context.task(), context.quiz());
    }

    @Operation(summary = "Get generated quiz questions for session")
    @GetMapping("/quiz")
    public SessionQuizResponse getQuiz(@PathVariable @Positive Long sessionId) {
        Long userId = UserContextHolder.getRequiredUserId();
        SessionQuizService.QuizContext context = sessionQuizService.getQuiz(sessionId, userId);
        return new SessionQuizResponse(
            sessionId,
            context.task().getId(),
            context.quiz().getId(),
            context.quiz().getGenerationStatus().name(),
            context.quiz().getStatus().name(),
            context.quiz().getQuestionCount(),
            context.quiz().getAnsweredCount(),
            context.quiz().getFailureReason(),
            context.items().stream().map(this::toQuestionResponse).toList()
        );
    }

    @Operation(summary = "Submit all quiz answers for session")
    @PostMapping("/quiz/submit")
    public SessionFeedbackResponse submit(
        @PathVariable @Positive Long sessionId,
        @Valid @RequestBody SessionQuizSubmitRequest request
    ) {
        Long userId = UserContextHolder.getRequiredUserId();
        return toFeedbackResponse(sessionQuizService.submit(sessionId, userId, request));
    }

    @Operation(summary = "Get structured feedback report for session quiz")
    @GetMapping("/feedback")
    public SessionFeedbackResponse getFeedback(@PathVariable @Positive Long sessionId) {
        Long userId = UserContextHolder.getRequiredUserId();
        return toFeedbackResponse(sessionQuizService.getFeedback(sessionId, userId));
    }

    @Operation(summary = "Apply next action based on feedback")
    @PostMapping("/next-action")
    public SessionFeedbackResponse nextAction(
        @PathVariable @Positive Long sessionId,
        @Valid @RequestBody ApplyPracticeFeedbackActionRequest request
    ) {
        Long userId = UserContextHolder.getRequiredUserId();
        return toFeedbackResponse(sessionQuizService.applyNextAction(sessionId, userId, request.action()));
    }

    private SessionQuizStatusResponse toStatusResponse(Task task, PracticeQuiz quiz) {
        return new SessionQuizStatusResponse(
            quiz.getSessionId(),
            task.getId(),
            quiz.getId(),
            quiz.getGenerationStatus().name(),
            quiz.getStatus().name(),
            quiz.getQuestionCount(),
            quiz.getAnsweredCount(),
            quiz.getFailureReason(),
            quiz.getGenerationStatus().name().equals("FAILED")
        );
    }

    private SessionQuizQuestionResponse toQuestionResponse(PracticeItem item) {
        return new SessionQuizQuestionResponse(
            item.getId(),
            item.getQuestionType().name(),
            item.getStem(),
            parseOptions(item.getOptionsJson()),
            item.getExplanation(),
            item.getDifficulty(),
            item.getStatus().name()
        );
    }

    private SessionFeedbackResponse toFeedbackResponse(SessionQuizService.FeedbackContext context) {
        PracticeFeedbackReport report = context.report();
        return new SessionFeedbackResponse(
            report.getId(),
            report.getQuizId(),
            report.getSessionId(),
            report.getTaskId(),
            report.getReportStatus().name(),
            report.getDiagnosisSummary(),
            context.questionResults().stream()
                .map(result -> new SessionQuestionResultResponse(
                    result.item().getId(),
                    result.item().getQuestionType().name(),
                    result.item().getStem(),
                    result.submission().getUserAnswer(),
                    result.submission().getScore(),
                    Boolean.TRUE.equals(result.submission().getCorrect()),
                    result.submission().getFeedback(),
                    parseArray(result.submission().getErrorTagsJson())
                ))
                .toList(),
            parseArray(report.getStrengthsJson()),
            parseArray(report.getWeaknessesJson()),
            parseArray(report.getReviewFocusJson()),
            report.getNextRoundAdvice(),
            report.getRecommendedAction(),
            report.getRecommendedAction(),
            report.getSelectedAction(),
            report.getSource()
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
