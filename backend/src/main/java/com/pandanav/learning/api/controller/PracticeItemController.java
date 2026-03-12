package com.pandanav.learning.api.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pandanav.learning.api.dto.practice.PracticeItemResponse;
import com.pandanav.learning.api.dto.practice.PracticeJudgementResponse;
import com.pandanav.learning.api.dto.practice.PracticeItemsResponse;
import com.pandanav.learning.api.dto.practice.PracticeSubmissionResponse;
import com.pandanav.learning.api.dto.practice.SubmitPracticeAnswerRequest;
import com.pandanav.learning.api.dto.practice.SubmitPracticeAnswerResponse;
import com.pandanav.learning.application.service.PracticeService;
import com.pandanav.learning.auth.UserContextHolder;
import com.pandanav.learning.domain.model.PracticeItem;
import com.pandanav.learning.domain.model.PracticeSubmission;
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
@RequestMapping({"/api/sessions/{sessionId}/tasks/{taskId}/practice-items", "/api/session/{sessionId}/tasks/{taskId}/practice-items"})
public class PracticeItemController {

    private final PracticeService practiceService;
    private final ObjectMapper objectMapper;

    public PracticeItemController(PracticeService practiceService, ObjectMapper objectMapper) {
        this.practiceService = practiceService;
        this.objectMapper = objectMapper;
    }

    @Operation(summary = "List practice items for training task")
    @GetMapping
    public PracticeItemsResponse list(
        @PathVariable @Positive Long sessionId,
        @PathVariable @Positive Long taskId
    ) {
        Long userId = UserContextHolder.getRequiredUserId();
        List<PracticeItemResponse> items = practiceService.listPracticeItems(sessionId, taskId, userId)
            .stream()
            .map(this::toResponse)
            .toList();
        return new PracticeItemsResponse(sessionId, taskId, items.size(), items);
    }

    @Operation(summary = "Generate practice items for training task")
    @PostMapping("/generate")
    public PracticeItemsResponse generate(
        @PathVariable @Positive Long sessionId,
        @PathVariable @Positive Long taskId
    ) {
        Long userId = UserContextHolder.getRequiredUserId();
        List<PracticeItemResponse> items = practiceService.getOrCreatePracticeItems(sessionId, taskId, userId)
            .stream()
            .map(this::toResponse)
            .toList();
        return new PracticeItemsResponse(sessionId, taskId, items.size(), items);
    }

    @Operation(summary = "Submit answer for one practice item")
    @PostMapping("/{practiceItemId}/submit")
    public SubmitPracticeAnswerResponse submit(
        @PathVariable @Positive Long sessionId,
        @PathVariable @Positive Long taskId,
        @PathVariable @Positive Long practiceItemId,
        @Valid @RequestBody SubmitPracticeAnswerRequest request
    ) {
        Long userId = UserContextHolder.getRequiredUserId();
        PracticeSubmission submission = practiceService.submitPracticeAnswer(
            sessionId,
            taskId,
            practiceItemId,
            userId,
            request.userAnswer()
        );
        PracticeItem practiceItem = practiceService.listPracticeItems(sessionId, taskId, userId).stream()
            .filter(item -> item.getId().equals(practiceItemId))
            .findFirst()
            .orElseThrow(() -> new InternalServerException("Practice item not found after submission."));

        List<String> errorTags = parseErrorTags(submission.getErrorTagsJson());
        PracticeSubmissionResponse submissionResponse = new PracticeSubmissionResponse(
            submission.getId(),
            submission.getPracticeItemId(),
            submission.getUserAnswer(),
            submission.getScore(),
            submission.getCorrect(),
            submission.getFeedback(),
            errorTags,
            submission.getSubmittedAt()
        );
        PracticeJudgementResponse judgement = new PracticeJudgementResponse(
            submission.getScore(),
            submission.getCorrect(),
            submission.getFeedback(),
            errorTags
        );
        return new SubmitPracticeAnswerResponse(submissionResponse, toResponse(practiceItem), judgement);
    }

    private PracticeItemResponse toResponse(PracticeItem item) {
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

    private List<String> parseErrorTags(String errorTagsJson) {
        if (errorTagsJson == null || errorTagsJson.isBlank()) {
            return List.of();
        }
        try {
            JsonNode parsed = objectMapper.readTree(errorTagsJson);
            if (!parsed.isArray()) {
                return List.of();
            }
            List<String> result = new ArrayList<>();
            for (JsonNode node : parsed) {
                if (node.isTextual()) {
                    result.add(node.asText());
                }
            }
            return result;
        } catch (Exception ex) {
            throw new InternalServerException("Stored error_tags_json is invalid.");
        }
    }
}
