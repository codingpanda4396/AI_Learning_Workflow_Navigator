package com.pandanav.learning.api.controller;

import com.pandanav.learning.api.dto.ApiErrorResponse;
import com.pandanav.learning.api.dto.session.CreateSessionRequest;
import com.pandanav.learning.api.dto.session.CreateSessionResponse;
import com.pandanav.learning.api.dto.session.PlanSessionResponse;
import com.pandanav.learning.api.dto.session.SessionOverviewResponse;
import com.pandanav.learning.application.usecase.CreateSessionUseCase;
import com.pandanav.learning.application.usecase.GetSessionOverviewUseCase;
import com.pandanav.learning.application.usecase.PlanSessionTasksUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
@RequestMapping("/api/session")
public class SessionController {

    private final CreateSessionUseCase createSessionUseCase;
    private final PlanSessionTasksUseCase planSessionTasksUseCase;
    private final GetSessionOverviewUseCase getSessionOverviewUseCase;

    public SessionController(
        CreateSessionUseCase createSessionUseCase,
        PlanSessionTasksUseCase planSessionTasksUseCase,
        GetSessionOverviewUseCase getSessionOverviewUseCase
    ) {
        this.createSessionUseCase = createSessionUseCase;
        this.planSessionTasksUseCase = planSessionTasksUseCase;
        this.getSessionOverviewUseCase = getSessionOverviewUseCase;
    }

    @Operation(summary = "Create learning session")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "400", description = "Bad Request",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Internal Error",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping("/create")
    public CreateSessionResponse createSession(@Valid @RequestBody CreateSessionRequest request) {
        return createSessionUseCase.execute(request);
    }

    @Operation(summary = "Plan session tasks")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "404", description = "Not Found",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Internal Error",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping("/{sessionId}/plan")
    public PlanSessionResponse planSession(@PathVariable @Positive Long sessionId) {
        return planSessionTasksUseCase.execute(sessionId);
    }

    @Operation(summary = "Get session overview")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "404", description = "Not Found",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Internal Error",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/{sessionId}/overview")
    public SessionOverviewResponse getOverview(@PathVariable @Positive Long sessionId) {
        return getSessionOverviewUseCase.execute(sessionId);
    }
}
