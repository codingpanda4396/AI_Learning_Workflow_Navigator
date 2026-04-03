package navigator.api.controller;

import jakarta.validation.Valid;
import navigator.api.GlobalResponse;
import navigator.api.dto.scaffold.CompleteConversationStageRequest;
import navigator.api.dto.scaffold.CompleteConversationStageResult;
import navigator.api.dto.scaffold.CompleteReflectionStageRequest;
import navigator.api.dto.scaffold.CompleteReflectionStageResult;
import navigator.api.dto.scaffold.CompleteStructureStageRequest;
import navigator.api.dto.scaffold.CompleteStructureStageResult;
import navigator.api.dto.scaffold.LearningScaffoldActionResult;
import navigator.api.dto.scaffold.StageScaffold;
import navigator.api.dto.scaffold.StructureSkeletonRequest;
import navigator.api.dto.scaffold.StructureSkeletonResult;
import navigator.api.dto.scaffold.SubmitLearningScaffoldActionRequest;
import navigator.application.scaffold.LearningScaffoldEngineService;
import navigator.application.scaffold.WorkbenchMode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tasks")
public class LearningScaffoldController {

    private final LearningScaffoldEngineService learningScaffoldEngineService;

    public LearningScaffoldController(LearningScaffoldEngineService learningScaffoldEngineService) {
        this.learningScaffoldEngineService = learningScaffoldEngineService;
    }

    /**
     * 查询当前脚手架阶段（勿传 stageKey，由引擎决定进度）。
     */
    @GetMapping("/{taskId}/learning-scaffold/stage/current")
    public GlobalResponse<StageScaffold> getCurrentStage(
            @PathVariable String taskId,
            @RequestParam String sessionId,
            @RequestParam(required = false, defaultValue = "full") String workbenchMode) {
        return GlobalResponse.ok(learningScaffoldEngineService.getStage(
                sessionId, taskId, null, WorkbenchMode.fromQueryParam(workbenchMode)));
    }

    /**
     * 与 {@link #getCurrentStage} 相同语义；若传 stageKey 须与引擎当前阶段一致。
     */
    @GetMapping("/{taskId}/learning-scaffold/stage")
    public GlobalResponse<StageScaffold> getStage(
            @PathVariable String taskId,
            @RequestParam String sessionId,
            @RequestParam(required = false) String stageKey,
            @RequestParam(required = false, defaultValue = "full") String workbenchMode) {
        return GlobalResponse.ok(learningScaffoldEngineService.getStage(
                sessionId, taskId, stageKey, WorkbenchMode.fromQueryParam(workbenchMode)));
    }

    @PostMapping("/{taskId}/learning-scaffold/action")
    public GlobalResponse<LearningScaffoldActionResult> submitAction(
            @PathVariable String taskId,
            @Valid @RequestBody SubmitLearningScaffoldActionRequest request) {
        return GlobalResponse.ok(learningScaffoldEngineService.submitAction(taskId, request));
    }

    @PostMapping("/{taskId}/learning-scaffold/structure/skeleton")
    public GlobalResponse<StructureSkeletonResult> generateStructureSkeleton(
            @PathVariable String taskId,
            @Valid @RequestBody StructureSkeletonRequest request) {
        return GlobalResponse.ok(learningScaffoldEngineService.generateStructureSkeleton(taskId, request));
    }

    @PostMapping("/{taskId}/learning-scaffold/structure/complete")
    public GlobalResponse<CompleteStructureStageResult> completeStructureStage(
            @PathVariable String taskId,
            @Valid @RequestBody CompleteStructureStageRequest request) {
        return GlobalResponse.ok(learningScaffoldEngineService.completeStructureStage(taskId, request));
    }

    @PostMapping("/{taskId}/learning-scaffold/conversation/complete")
    public GlobalResponse<CompleteConversationStageResult> completeConversationStage(
            @PathVariable String taskId,
            @Valid @RequestBody CompleteConversationStageRequest request) {
        return GlobalResponse.ok(learningScaffoldEngineService.completeConversationStage(taskId, request));
    }

    @PostMapping("/{taskId}/learning-scaffold/reflection/complete")
    public GlobalResponse<CompleteReflectionStageResult> completeReflectionStage(
            @PathVariable String taskId,
            @Valid @RequestBody CompleteReflectionStageRequest request) {
        return GlobalResponse.ok(learningScaffoldEngineService.completeReflectionStage(taskId, request));
    }
}
