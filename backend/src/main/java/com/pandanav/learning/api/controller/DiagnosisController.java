package com.pandanav.learning.api.controller;

import com.pandanav.learning.api.dto.ApiEnvelope;
import com.pandanav.learning.api.dto.ApiErrorResponse;
import com.pandanav.learning.api.dto.diagnosis.GenerateDiagnosisRequest;
import com.pandanav.learning.api.dto.diagnosis.GenerateDiagnosisResponse;
import com.pandanav.learning.api.dto.diagnosis.SubmitDiagnosisRequest;
import com.pandanav.learning.api.dto.diagnosis.SubmitDiagnosisResponse;
import com.pandanav.learning.application.service.DiagnosisService;
import com.pandanav.learning.auth.UserContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
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

    @Operation(summary = "Generate diagnosis questions")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "400", description = "Bad Request",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Not Found",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping("/generate")
    public ApiEnvelope<GenerateDiagnosisResponse> generate(@Valid @RequestBody GenerateDiagnosisRequest request) {
        GenerateDiagnosisResponse response = diagnosisService.generateDiagnosis(request.sessionId(), UserContextHolder.getRequiredUserId());
        return ApiEnvelope.ok(response, response.contentSource());
    }

    @Operation(summary = "Submit diagnosis answers")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "400", description = "Bad Request",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Not Found",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping("/submit")
    public ApiEnvelope<SubmitDiagnosisResponse> submit(@Valid @RequestBody SubmitDiagnosisRequest request) {
        SubmitDiagnosisResponse response = diagnosisService.submitDiagnosis(request, UserContextHolder.getRequiredUserId());
        return ApiEnvelope.ok(response, response.contentSource());
    }
}
