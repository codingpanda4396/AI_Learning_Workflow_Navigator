package com.pandanav.learning.api.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pandanav.learning.api.dto.practice.PracticeItemResponse;
import com.pandanav.learning.api.dto.practice.PracticeItemsResponse;
import com.pandanav.learning.application.service.PracticeService;
import com.pandanav.learning.auth.UserContextHolder;
import com.pandanav.learning.domain.model.PracticeItem;
import com.pandanav.learning.infrastructure.exception.InternalServerException;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/session/{sessionId}/tasks/{taskId}/practice-items")
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
}
