package com.panda.ainavigator.api.controller;

import com.panda.ainavigator.api.dto.task.RunTaskResponse;
import com.panda.ainavigator.api.dto.task.SubmitTaskRequest;
import com.panda.ainavigator.api.dto.task.SubmitTaskResponse;
import com.panda.ainavigator.application.service.TaskApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/task")
public class TaskController {

    private final TaskApplicationService taskService;

    public TaskController(TaskApplicationService taskService) {
        this.taskService = taskService;
    }

    @Operation(summary = "Run task")
    @PostMapping("/{taskId}/run")
    public RunTaskResponse runTask(@PathVariable Long taskId) {
        return taskService.runTask(taskId);
    }

    @Operation(summary = "Submit training answer")
    @PostMapping("/{taskId}/submit")
    public SubmitTaskResponse submitTask(@PathVariable Long taskId,
                                         @Valid @RequestBody SubmitTaskRequest request) {
        return taskService.submitTask(taskId, request);
    }
}
