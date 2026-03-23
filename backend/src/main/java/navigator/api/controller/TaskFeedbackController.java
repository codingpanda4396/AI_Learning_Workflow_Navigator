package navigator.api.controller;

import jakarta.validation.Valid;
import navigator.api.GlobalResponse;
import navigator.api.dto.TaskFeedbackRequest;
import navigator.api.dto.TaskFeedbackResponse;
import navigator.application.TaskFeedbackService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/task")
public class TaskFeedbackController {

    private final TaskFeedbackService taskFeedbackService;

    public TaskFeedbackController(TaskFeedbackService taskFeedbackService) {
        this.taskFeedbackService = taskFeedbackService;
    }

    @PostMapping("/feedback")
    public GlobalResponse<TaskFeedbackResponse> feedback(
            @Valid @RequestBody TaskFeedbackRequest request) {
        TaskFeedbackResponse data = taskFeedbackService.evaluate(request.getAnswer());
        return GlobalResponse.ok(data);
    }
}
