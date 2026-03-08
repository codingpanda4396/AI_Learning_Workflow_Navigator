package com.pandanav.learning.api.controller;

import com.pandanav.learning.api.dto.session.SessionHistoryResponse;
import com.pandanav.learning.api.dto.session.SessionOverviewResponse;
import com.pandanav.learning.application.service.SessionHistoryService;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/session")
public class SessionHistoryController {

    private final SessionHistoryService sessionHistoryService;

    public SessionHistoryController(SessionHistoryService sessionHistoryService) {
        this.sessionHistoryService = sessionHistoryService;
    }

    @GetMapping("/history")
    public SessionHistoryResponse getHistory(
        @RequestParam(value = "page", required = false) Integer page,
        @RequestParam(value = "page_size", required = false) Integer pageSize,
        @RequestParam(value = "status", required = false) String status
    ) {
        return sessionHistoryService.listHistory(page, pageSize, status);
    }

    @GetMapping("/{sessionId}")
    public SessionOverviewResponse getSessionDetail(@PathVariable @Positive Long sessionId) {
        return sessionHistoryService.getSessionDetail(sessionId);
    }

    @PostMapping("/{sessionId}/resume")
    public SessionOverviewResponse resume(@PathVariable @Positive Long sessionId) {
        return sessionHistoryService.resume(sessionId);
    }
}
