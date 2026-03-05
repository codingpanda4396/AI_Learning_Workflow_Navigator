package com.panda.ainavigator.api.controller;

import com.panda.ainavigator.api.dto.session.*;
import com.panda.ainavigator.application.service.SessionApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/session")
public class SessionController {

    private final SessionApplicationService sessionService;

    public SessionController(SessionApplicationService sessionService) {
        this.sessionService = sessionService;
    }

    @Operation(summary = "Create learning session")
    @PostMapping("/create")
    public CreateSessionResponse createSession(@Valid @RequestBody CreateSessionRequest request) {
        return sessionService.createSession(request);
    }

    @Operation(summary = "Plan session tasks")
    @PostMapping("/{sessionId}/plan")
    public PlanSessionResponse planSession(@PathVariable Long sessionId) {
        return sessionService.planSession(sessionId);
    }

    @Operation(summary = "Get session overview")
    @GetMapping("/{sessionId}/overview")
    public SessionOverviewResponse overview(@PathVariable Long sessionId) {
        return sessionService.overview(sessionId);
    }
}
