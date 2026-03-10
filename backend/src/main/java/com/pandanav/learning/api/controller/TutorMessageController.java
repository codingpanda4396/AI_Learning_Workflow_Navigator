package com.pandanav.learning.api.controller;

import com.pandanav.learning.api.dto.ApiErrorResponse;
import com.pandanav.learning.api.dto.tutor.TutorMessageListResponse;
import com.pandanav.learning.api.dto.tutor.TutorSendMessageRequest;
import com.pandanav.learning.api.dto.tutor.TutorSendMessageResponse;
import com.pandanav.learning.application.service.TutorMessageService;
import com.pandanav.learning.auth.UserContextHolder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.validation.annotation.Validated;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Validated
@RestController
@RequestMapping("/api/session/{sessionId}/tasks/{taskId}/tutor/messages")
public class TutorMessageController {

    private final TutorMessageService tutorMessageService;

    public TutorMessageController(TutorMessageService tutorMessageService) {
        this.tutorMessageService = tutorMessageService;
    }

    @Operation(summary = "Get tutor message history")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "404", description = "Not Found",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping
    public TutorMessageListResponse getMessages(
        @PathVariable @Positive Long sessionId,
        @PathVariable @Positive Long taskId
    ) {
        return tutorMessageService.listMessages(sessionId, taskId, UserContextHolder.getRequiredUserId());
    }

    @Operation(summary = "Send tutor message")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "400", description = "Bad Request",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Not Found",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping
    public TutorSendMessageResponse sendMessage(
        @PathVariable @Positive Long sessionId,
        @PathVariable @Positive Long taskId,
        @Valid @RequestBody TutorSendMessageRequest request
    ) {
        return tutorMessageService.sendMessage(
            sessionId,
            taskId,
            UserContextHolder.getRequiredUserId(),
            request.content()
        );
    }

    @PostMapping(path = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamMessage(
        @PathVariable @Positive Long sessionId,
        @PathVariable @Positive Long taskId,
        @Valid @RequestBody TutorSendMessageRequest request
    ) {
        return tutorMessageService.streamMessage(
            sessionId,
            taskId,
            UserContextHolder.getRequiredUserId(),
            request.content()
        );
    }
}
