package com.pandanav.learning.api.controller;

import com.pandanav.learning.api.dto.ApiErrorResponse;
import com.pandanav.learning.api.dto.diagnosis.CapabilityProfileResponse;
import com.pandanav.learning.application.service.CapabilityProfileQueryService;
import com.pandanav.learning.auth.UserContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/capability-profile")
public class CapabilityProfileController {

    private final CapabilityProfileQueryService capabilityProfileQueryService;

    public CapabilityProfileController(CapabilityProfileQueryService capabilityProfileQueryService) {
        this.capabilityProfileQueryService = capabilityProfileQueryService;
    }

    @Operation(summary = "Get latest capability profile for session")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "404", description = "Not Found",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/{sessionId}")
    public CapabilityProfileResponse getBySessionId(@PathVariable @Positive Long sessionId) {
        return capabilityProfileQueryService.getLatestProfile(sessionId, UserContextHolder.getRequiredUserId());
    }
}
