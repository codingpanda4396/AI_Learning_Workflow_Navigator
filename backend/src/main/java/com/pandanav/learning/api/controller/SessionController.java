package com.pandanav.learning.api.controller;

import com.pandanav.learning.api.dto.ApiErrorResponse;
import com.pandanav.learning.api.dto.session.CurrentSessionResponse;
import com.pandanav.learning.api.dto.session.CreateSessionRequest;
import com.pandanav.learning.api.dto.session.CreateSessionResponse;
import com.pandanav.learning.api.dto.session.GoalDiagnosisRequest;
import com.pandanav.learning.api.dto.session.GoalDiagnosisResponse;
import com.pandanav.learning.api.dto.session.PathResponse;
import com.pandanav.learning.api.dto.session.PathOptionsResponse;
import com.pandanav.learning.api.dto.session.PlanSessionResponse;
import com.pandanav.learning.api.dto.session.SessionOverviewResponse;
import com.pandanav.learning.application.service.GoalDiagnosisService;
import com.pandanav.learning.application.service.PathOptionsService;
import com.pandanav.learning.application.usecase.CreateSessionUseCase;
import com.pandanav.learning.application.usecase.GetCurrentSessionUseCase;
import com.pandanav.learning.application.usecase.GetSessionOverviewUseCase;
import com.pandanav.learning.application.usecase.GetSessionPathUseCase;
import com.pandanav.learning.application.usecase.PlanSessionTasksUseCase;
import com.pandanav.learning.auth.UserContextHolder;
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
@RequestMapping({"/api/session", "/api/workflow"})
public class SessionController {

    private final CreateSessionUseCase createSessionUseCase;
    private final PlanSessionTasksUseCase planSessionTasksUseCase;
    private final GetSessionOverviewUseCase getSessionOverviewUseCase;
    private final GetCurrentSessionUseCase getCurrentSessionUseCase;
    private final GetSessionPathUseCase getSessionPathUseCase;
    private final GoalDiagnosisService goalDiagnosisService;
    private final PathOptionsService pathOptionsService;

    public SessionController(
        CreateSessionUseCase createSessionUseCase,
        PlanSessionTasksUseCase planSessionTasksUseCase,
        GetSessionOverviewUseCase getSessionOverviewUseCase,
        GetCurrentSessionUseCase getCurrentSessionUseCase,
        GetSessionPathUseCase getSessionPathUseCase,
        GoalDiagnosisService goalDiagnosisService,
        PathOptionsService pathOptionsService
    ) {
        this.createSessionUseCase = createSessionUseCase;
        this.planSessionTasksUseCase = planSessionTasksUseCase;
        this.getSessionOverviewUseCase = getSessionOverviewUseCase;
        this.getCurrentSessionUseCase = getCurrentSessionUseCase;
        this.getSessionPathUseCase = getSessionPathUseCase;
        this.goalDiagnosisService = goalDiagnosisService;
        this.pathOptionsService = pathOptionsService;
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
        return createSessionUseCase.execute(request, UserContextHolder.getRequiredUserId());
    }

    @Operation(summary = "Diagnose learning goal quality")
    @PostMapping("/goal-diagnose")
    public GoalDiagnosisResponse diagnoseGoal(@Valid @RequestBody GoalDiagnosisRequest request) {
        return goalDiagnosisService.diagnose(request);
    }

    @Operation(summary = "Get learning path options")
    @PostMapping("/path-options")
    public PathOptionsResponse pathOptions(@Valid @RequestBody GoalDiagnosisRequest request) {
        return pathOptionsService.buildOptions(request);
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

    @Operation(summary = "Get learning path for session")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "404", description = "Not Found",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Internal Error",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/{sessionId}/path")
    public PathResponse getPath(@PathVariable @Positive Long sessionId) {
        return getSessionPathUseCase.execute(sessionId);
    }

    @Operation(summary = "Get current session by user id")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "500", description = "Internal Error",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/current")
    public CurrentSessionResponse getCurrentSession() {
        return getCurrentSessionUseCase.execute(UserContextHolder.getRequiredUserId());
    }
}
