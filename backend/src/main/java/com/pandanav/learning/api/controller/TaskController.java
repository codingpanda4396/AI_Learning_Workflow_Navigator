package com.pandanav.learning.api.controller;

import com.pandanav.learning.api.dto.ApiErrorResponse;
import com.pandanav.learning.api.dto.task.RunTaskResponse;
import com.pandanav.learning.api.dto.task.SubmitTaskRequest;
import com.pandanav.learning.api.dto.task.SubmitTaskResponse;
import com.pandanav.learning.api.dto.task.TaskDetailResponse;
import com.pandanav.learning.application.service.TaskQueryService;
import com.pandanav.learning.application.usecase.RunTaskUseCase;
import com.pandanav.learning.application.usecase.SubmitTrainingAnswerUseCase;
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
@RequestMapping("/api/task")
public class TaskController {

    private final TaskQueryService taskQueryService;
    private final RunTaskUseCase runTaskUseCase;
    private final SubmitTrainingAnswerUseCase submitTrainingAnswerUseCase;

    public TaskController(
        TaskQueryService taskQueryService,
        RunTaskUseCase runTaskUseCase,
        SubmitTrainingAnswerUseCase submitTrainingAnswerUseCase
    ) {
        this.taskQueryService = taskQueryService;
        this.runTaskUseCase = runTaskUseCase;
        this.submitTrainingAnswerUseCase = submitTrainingAnswerUseCase;
    }

    @Operation(summary = "Get task detail")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "404", description = "Not Found",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Internal Error",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @GetMapping("/{taskId}")
    public TaskDetailResponse getTaskDetail(@PathVariable @Positive Long taskId) {
        return taskQueryService.getTaskDetail(taskId);
    }

    @Operation(summary = "Run task")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "404", description = "Not Found",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Internal Error",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping("/{taskId}/run")
    public RunTaskResponse runTask(@PathVariable @Positive Long taskId) {
        return runTaskUseCase.run(taskId);
    }

    @Operation(summary = "Submit training answer")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "400", description = "Bad Request",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "404", description = "Not Found",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "409", description = "Conflict",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class))),
        @ApiResponse(responseCode = "500", description = "Internal Error",
            content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    })
    @PostMapping("/{taskId}/submit")
    public SubmitTaskResponse submitTask(
        @PathVariable @Positive Long taskId,
        @Valid @RequestBody SubmitTaskRequest request
    ) {
        return submitTrainingAnswerUseCase.submit(taskId, request);
    }
}


