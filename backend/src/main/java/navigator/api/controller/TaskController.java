package navigator.api.controller;

import jakarta.validation.Valid;
import navigator.api.GlobalResponse;
import navigator.api.dto.CheckpointRequest;
import navigator.api.dto.CheckpointResponse;
import navigator.api.dto.CompleteTaskData;
import navigator.api.dto.CompleteTaskRequest;
import navigator.api.dto.SelfExplanationRequest;
import navigator.api.dto.SelfExplanationResponse;
import navigator.api.dto.TaskInteractionData;
import navigator.api.dto.TaskInteractionRequest;
import navigator.api.dto.TaskExecutionSummaryData;
import navigator.api.dto.TaskMessageRequest;
import navigator.api.dto.TaskMessageResponse;
import navigator.api.dto.scaffold.StageScaffold;
import navigator.application.ExecutionApplicationService;
import navigator.application.scaffold.LearningScaffoldEngineService;
import navigator.application.scaffold.WorkbenchMode;
import navigator.application.task.TaskExecutionFlowService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final ExecutionApplicationService executionService;
    private final TaskExecutionFlowService taskExecutionFlowService;
    private final LearningScaffoldEngineService learningScaffoldEngineService;

    public TaskController(ExecutionApplicationService executionService,
                          TaskExecutionFlowService taskExecutionFlowService,
                          LearningScaffoldEngineService learningScaffoldEngineService) {
        this.executionService = executionService;
        this.taskExecutionFlowService = taskExecutionFlowService;
        this.learningScaffoldEngineService = learningScaffoldEngineService;
    }

    /**
     * 按阶段加载脚手架 UI。stage 须与引擎当前阶段一致。
     * workbenchMode=fast：仅动作卡组装工作台，不调 LLM；full（默认）：含软文案。
     */
    @GetMapping("/{taskId}/scaffold")
    public GlobalResponse<StageScaffold> getScaffold(
            @PathVariable String taskId,
            @RequestParam String sessionId,
            @RequestParam String stage,
            @RequestParam(required = false, defaultValue = "full") String workbenchMode) {
        return GlobalResponse.ok(learningScaffoldEngineService.getStage(
                sessionId, taskId, stage, WorkbenchMode.fromQueryParam(workbenchMode)));
    }

    @PostMapping("/{taskId}/messages")
    public GlobalResponse<TaskMessageResponse> postMessage(
            @PathVariable String taskId,
            @Valid @RequestBody TaskMessageRequest request) {
        TaskMessageResponse data = taskExecutionFlowService.postMessage(taskId, request);
        return GlobalResponse.ok(data);
    }

    @GetMapping("/{taskId}/execution-summary")
    public GlobalResponse<TaskExecutionSummaryData> getExecutionSummary(
            @PathVariable String taskId,
            @RequestParam String sessionId) {
        return GlobalResponse.ok(taskExecutionFlowService.getExecutionSummary(sessionId, taskId));
    }

    @PostMapping("/{taskId}/self-explanation")
    public GlobalResponse<SelfExplanationResponse> postSelfExplanation(
            @PathVariable String taskId,
            @Valid @RequestBody SelfExplanationRequest request) {
        return GlobalResponse.ok(taskExecutionFlowService.postSelfExplanation(
                taskId, request.getSessionId(), request.getContent()));
    }

    @PostMapping("/{taskId}/checkpoint")
    public GlobalResponse<CheckpointResponse> postCheckpoint(
            @PathVariable String taskId,
            @Valid @RequestBody CheckpointRequest request) {
        return GlobalResponse.ok(taskExecutionFlowService.postCheckpoint(
                taskId, request.getSessionId(), request.getAnswer()));
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
