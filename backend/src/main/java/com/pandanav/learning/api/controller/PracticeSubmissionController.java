package com.pandanav.learning.api.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pandanav.learning.api.dto.practice.PracticeSubmissionResponse;
import com.pandanav.learning.api.dto.practice.PracticeSubmissionsResponse;
import com.pandanav.learning.application.service.PracticeService;
import com.pandanav.learning.auth.UserContextHolder;
import com.pandanav.learning.domain.model.PracticeSubmission;
import com.pandanav.learning.infrastructure.exception.InternalServerException;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@Validated
@RestController
@RequestMapping("/api/session/{sessionId}/tasks/{taskId}/practice-submissions")
public class PracticeSubmissionController {

    private final PracticeService practiceService;
    private final ObjectMapper objectMapper;

    public PracticeSubmissionController(PracticeService practiceService, ObjectMapper objectMapper) {
        this.practiceService = practiceService;
        this.objectMapper = objectMapper;
    }

    @Operation(summary = "List practice submissions for training task")
    @GetMapping
    public PracticeSubmissionsResponse list(
        @PathVariable @Positive Long sessionId,
        @PathVariable @Positive Long taskId
    ) {
        Long userId = UserContextHolder.getRequiredUserId();
        List<PracticeSubmissionResponse> submissions = practiceService.listPracticeSubmissions(sessionId, taskId, userId)
            .stream()
            .map(this::toResponse)
            .toList();
        return new PracticeSubmissionsResponse(sessionId, taskId, submissions.size(), submissions);
    }

    private PracticeSubmissionResponse toResponse(PracticeSubmission submission) {
        return new PracticeSubmissionResponse(
            submission.getId(),
            submission.getPracticeItemId(),
            submission.getUserAnswer(),
            submission.getScore(),
            submission.getCorrect(),
            submission.getFeedback(),
            parseErrorTags(submission.getErrorTagsJson()),
            submission.getSubmittedAt()
        );
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
