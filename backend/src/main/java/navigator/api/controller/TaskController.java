package navigator.api.controller;

import jakarta.validation.Valid;
import navigator.api.GlobalResponse;
import navigator.api.dto.CompleteTaskData;
import navigator.api.dto.CompleteTaskRequest;
import navigator.api.dto.TaskInteractionData;
import navigator.api.dto.TaskInteractionRequest;
import navigator.application.ExecutionApplicationService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final ExecutionApplicationService executionService;

    public TaskController(ExecutionApplicationService executionService) {
        this.executionService = executionService;
    }

    @PostMapping("/{taskId}/interactions")
    public GlobalResponse<TaskInteractionData> recordInteraction(
            @PathVariable String taskId,
            @Valid @RequestBody TaskInteractionRequest request) {
        TaskInteractionData data = executionService.recordInteraction(taskId, request);
        return GlobalResponse.ok(data);
    }

    @PostMapping("/{taskId}/complete")
    public GlobalResponse<CompleteTaskData> completeTask(
            @PathVariable String taskId,
            @Valid @RequestBody CompleteTaskRequest request) {
        CompleteTaskData data = executionService.completeTask(taskId, request);
        if (data == null) return GlobalResponse.notFound("session not found");
        return GlobalResponse.ok(data);
    }
}
