package com.pandanav.learning.api.controller;

import com.pandanav.learning.api.dto.ApiEnvelope;
import com.pandanav.learning.api.dto.ApiErrorResponse;
import com.pandanav.learning.api.dto.diagnosis.CreateDiagnosisSessionRequest;
import com.pandanav.learning.api.dto.diagnosis.CreateDiagnosisSessionResponse;
import com.pandanav.learning.api.dto.diagnosis.SubmitDiagnosisSessionRequest;
import com.pandanav.learning.api.dto.diagnosis.SubmitDiagnosisSessionResponse;
import com.pandanav.learning.application.service.DiagnosisService;
import com.pandanav.learning.auth.UserContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/diagnosis")
public class DiagnosisController {

    private final DiagnosisService diagnosisService;

    public DiagnosisController(DiagnosisService diagnosisService) {
        this.diagnosisService = diagnosisService;
    }

    @Operation(summary = "Create diagnosis session")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "400", description = "Bad Request",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Not Found",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping("/sessions")
    public ApiEnvelope<CreateDiagnosisSessionResponse> createSession(@Valid @RequestBody CreateDiagnosisSessionRequest request) {
        CreateDiagnosisSessionResponse response = diagnosisService.createDiagnosisSession(request.sessionId(), UserContextHolder.getRequiredUserId());
        return ApiEnvelope.ok(response, response.fallback().contentSource());
    }

    @Operation(summary = "Submit diagnosis answers")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "400", description = "Bad Request",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Not Found",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping("/sessions/{diagnosisId}/submissions")
    public ApiEnvelope<SubmitDiagnosisSessionResponse> submitSession(
        @PathVariable Long diagnosisId,
        @Valid @RequestBody SubmitDiagnosisSessionRequest request
    ) {
        SubmitDiagnosisSessionResponse response =
            diagnosisService.submitDiagnosisSession(diagnosisId, request, UserContextHolder.getRequiredUserId());
        return ApiEnvelope.ok(response, response.fallback().contentSource());
    }

    @Deprecated
    @PostMapping("/generate")
    public ApiEnvelope<CreateDiagnosisSessionResponse> generate(@Valid @RequestBody CreateDiagnosisSessionRequest request) {
        return createSession(request);
    }

    @Deprecated
    @PostMapping("/submit")
    public ApiEnvelope<SubmitDiagnosisSessionResponse> submit(@Valid @RequestBody SubmitDiagnosisSessionRequest request) {
        SubmitDiagnosisSessionResponse response =
            diagnosisService.submitDiagnosisSession(request.diagnosisId(), request, UserContextHolder.getRequiredUserId());
        return ApiEnvelope.ok(response, response.fallback().contentSource());
    }
}
