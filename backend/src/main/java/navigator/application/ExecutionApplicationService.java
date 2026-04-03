package navigator.application;

import navigator.api.dto.CompleteTaskData;
import navigator.api.dto.CompleteTaskRequest;
import navigator.api.dto.CurrentTaskData;
import navigator.api.dto.CurrentTaskGuidanceData;
import navigator.api.dto.SessionFlowStateData;
import navigator.api.dto.TaskInteractionData;
import navigator.application.execution.ExecutionSessionStateService;
import navigator.application.guard.SessionStateGuard;
import navigator.application.scaffold.LearningScaffoldEngineService;
import navigator.application.support.ExternalIdSupport;
import navigator.application.task.TaskCompletionApplicationService;
import navigator.application.task.TaskExecutionEventIngestService;
import navigator.application.task.TaskExecutionFlowService;
import navigator.domain.enums.LearningSessionStatus;
import navigator.domain.enums.LearningSessionStatusSupport;
import navigator.domain.model.TaskBlueprint;
import navigator.infrastructure.memory.InMemoryStore;
import navigator.infrastructure.persistence.repository.LearningSessionRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ExecutionApplicationService {

    private final SessionStateGuard sessionStateGuard;
    private final LearningSessionRepository learningSessionRepository;
    private final TaskExecutionFlowService taskExecutionFlowService;
    private final LearningScaffoldEngineService learningScaffoldEngineService;
    private final TaskExecutionEventIngestService taskExecutionEventIngestService;
    private final TaskCompletionApplicationService taskCompletionApplicationService;
    private final ExecutionSessionStateService executionSessionStateService;

    public ExecutionApplicationService(SessionStateGuard sessionStateGuard,
                                         LearningSessionRepository learningSessionRepository,
                                         TaskExecutionFlowService taskExecutionFlowService,
                                         LearningScaffoldEngineService learningScaffoldEngineService,
                                         TaskExecutionEventIngestService taskExecutionEventIngestService,
                                         TaskCompletionApplicationService taskCompletionApplicationService,
                                         ExecutionSessionStateService executionSessionStateService) {
        this.sessionStateGuard = sessionStateGuard;
        this.learningSessionRepository = learningSessionRepository;
        this.taskExecutionFlowService = taskExecutionFlowService;
        this.learningScaffoldEngineService = learningScaffoldEngineService;
        this.taskExecutionEventIngestService = taskExecutionEventIngestService;
        this.taskCompletionApplicationService = taskCompletionApplicationService;
        this.executionSessionStateService = executionSessionStateService;
    }

    public CurrentTaskGuidanceData getCurrentTaskGuidance(String sessionId) {
        return taskExecutionFlowService.getCurrentTaskGuidance(sessionId);
    }

    public CurrentTaskData getCurrentTask(String sessionId) {
        sessionStateGuard.requireSessionInProgressWithCommittedPlan(sessionId);
        InMemoryStore.LearningSessionState state = executionSessionStateService.loadOrHydrateLearningSession(sessionId);
        if (state == null || state.getTaskSequence() == null) {
            return null;
        }
        int idx = state.getCurrentTaskIndex();
        List<String> seq = state.getTaskSequence();
        if (idx >= seq.size()) {
            return null;
        }
        String taskId = seq.get(idx);
        TaskBlueprint blueprint = executionSessionStateService.resolveBlueprint(sessionId, state.getPlanId(), taskId);
        if (blueprint == null) {
            return null;
        }
        var meta = learningScaffoldEngineService.resolveTaskExecutionMeta(sessionId, taskId);
        CurrentTaskData.ProgressItem progress = CurrentTaskData.ProgressItem.builder()
                .currentIndex(idx + 1)
                .totalTasks(seq.size())
                .build();
        return CurrentTaskData.builder()
                .sessionId(sessionId)
                .taskId(taskId)
                .knowledge(meta.getKnowledge())
                .currentStage(meta.getCurrentStage())
                .progressMap(meta.getProgressMap())
                .progress(progress)
                .build();
    }

    public TaskInteractionData recordInteraction(String taskId, navigator.api.dto.TaskInteractionRequest request) {
        return taskExecutionEventIngestService.ingestLegacyInteraction(taskId, request);
    }

    public SessionFlowStateData getSessionFlowState(String sessionId) {
        InMemoryStore.LearningSessionState state = executionSessionStateService.loadOrHydrateLearningSession(sessionId);
        Long sessionDbId = ExternalIdSupport.extractNumericId(sessionId);
        var sessionEntity = sessionDbId != null ? learningSessionRepository.findById(sessionDbId) : null;
        if (state == null && sessionEntity == null) {
            return null;
        }

        String rawStatus = state != null && state.getStatus() != null
                ? state.getStatus()
                : sessionEntity != null ? sessionEntity.getStatus() : null;

        boolean hasCommittedPlan = (state != null && state.getPlanId() != null)
                || (sessionEntity != null && sessionEntity.getPlanId() != null);

        int totalTasks = 0;
        int completedTasks = 0;
        String currentTaskId = null;

        if (state != null && state.getTaskSequence() != null && !state.getTaskSequence().isEmpty()) {
            totalTasks = state.getTaskSequence().size();
            completedTasks = Math.max(0, Math.min(state.getCurrentTaskIndex(), totalTasks));
            if (completedTasks < totalTasks) {
                currentTaskId = state.getTaskSequence().get(completedTasks);
            }
        } else if (sessionEntity != null) {
            totalTasks = sessionEntity.getTotalTaskCount() != null ? sessionEntity.getTotalTaskCount() : 0;
            completedTasks = sessionEntity.getCompletedTaskCount() != null
                    ? Math.min(sessionEntity.getCompletedTaskCount(), Math.max(totalTasks, 0))
                    : 0;
        }

        boolean reportReady = LearningSessionStatusSupport.isReportReady(rawStatus, completedTasks, totalTasks);
        if (reportReady) {
            currentTaskId = null;
        }

        String currentPhase;
        String currentRoute;
        String sessionStatus;

        if (reportReady) {
            currentPhase = "report";
            currentRoute = "/report";
            sessionStatus = LearningSessionStatus.REPORT_READY.name();
        } else if (hasCommittedPlan) {
            currentPhase = "task";
            currentRoute = currentTaskId != null ? "/tasks/" + currentTaskId + "/run" : "/execution";
            sessionStatus = LearningSessionStatus.TASK_ACTIVE.name();
        } else if (LearningSessionStatusSupport.isDiagnosisCompleted(rawStatus)) {
            currentPhase = "plan";
            currentRoute = "/plan";
            sessionStatus = "PLAN_ACTIVE";
        } else {
            currentPhase = "diagnosis";
            currentRoute = "/diagnosis";
            sessionStatus = LearningSessionStatus.DIAGNOSIS_READY.name();
        }

        return SessionFlowStateData.builder()
                .sessionId(sessionId)
                .sessionStatus(sessionStatus)
                .currentPhase(currentPhase)
                .currentRoute(currentRoute)
                .currentTaskId(currentTaskId)
                .reportReady(reportReady)
                .completedTaskCount(completedTasks)
                .totalTaskCount(totalTasks)
                .build();
    }

    public CompleteTaskData completeTask(String taskId, CompleteTaskRequest request) {
        return taskCompletionApplicationService.completeTask(taskId, request);
    }
}
