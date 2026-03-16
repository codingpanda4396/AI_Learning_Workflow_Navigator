package navigator.application;

import navigator.api.dto.CompleteTaskData;
import navigator.api.dto.CurrentTaskData;
import navigator.api.dto.TaskInteractionData;
import navigator.application.guard.SessionStateGuard;
import navigator.application.guard.TaskProgressGuard;
import navigator.domain.enums.LearningSessionStatus;
import navigator.domain.enums.TaskCompletionStatus;
import navigator.domain.model.LearningPlanPreview;
import navigator.domain.model.TaskBlueprint;
import navigator.domain.model.TaskExecutionRecord;
import navigator.infrastructure.memory.InMemoryStore;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ExecutionApplicationService {

    private final InMemoryStore store;
    private final SessionStateGuard sessionStateGuard;
    private final TaskProgressGuard taskProgressGuard;

    public ExecutionApplicationService(InMemoryStore store, SessionStateGuard sessionStateGuard, TaskProgressGuard taskProgressGuard) {
        this.store = store;
        this.sessionStateGuard = sessionStateGuard;
        this.taskProgressGuard = taskProgressGuard;
    }

    public CurrentTaskData getCurrentTask(String sessionId) {
        sessionStateGuard.requireSessionInProgressWithCommittedPlan(sessionId);
        InMemoryStore.LearningSessionState state = store.getSessions().get(sessionId);
        if (state.getTaskSequence() == null) {
            return null;
        }
        int idx = state.getCurrentTaskIndex();
        List<String> seq = state.getTaskSequence();
        if (idx >= seq.size()) {
            return null;
        }
        String taskId = seq.get(idx);
        TaskBlueprint blueprint = resolveBlueprint(state.getPlanId(), taskId);
        if (blueprint == null) return null;
        CurrentTaskData.CurrentTaskItem item = CurrentTaskData.CurrentTaskItem.builder()
                .taskId(blueprint.getTaskId())
                .title(blueprint.getTitle())
                .taskType(blueprint.getTaskType().name())
                .goal(blueprint.getGoal())
                .whyThisTask(blueprint.getPromptScaffold() != null ? blueprint.getPromptScaffold() : FixedSampleData.whyThisTask(taskId))
                .estimatedMinutes(blueprint.getEstimatedMinutes())
                .promptScaffold(blueprint.getPromptScaffold())
                .completionCriteria(blueprint.getCompletionCriteria())
                .fallbackAction(blueprint.getFallbackAction())
                .build();
        CurrentTaskData.ProgressItem progress = CurrentTaskData.ProgressItem.builder()
                .currentIndex(idx + 1)
                .totalTasks(seq.size())
                .build();
        return CurrentTaskData.builder()
                .sessionId(sessionId)
                .currentTask(item)
                .progress(progress)
                .build();
    }

    public TaskInteractionData recordInteraction(String taskId, navigator.api.dto.TaskInteractionRequest request) {
        // Sprint 0: 仅接受，不做复杂统计
        return TaskInteractionData.builder()
                .taskId(taskId)
                .accepted(true)
                .build();
    }

    public CompleteTaskData completeTask(String taskId, navigator.api.dto.CompleteTaskRequest request) {
        String sessionId = request.getSessionId();
        taskProgressGuard.requireTaskCanComplete(sessionId, taskId);
        InMemoryStore.LearningSessionState state = store.getSessions().get(sessionId);
        TaskBlueprint blueprint = resolveBlueprint(state.getPlanId(), taskId);
        String taskTypeName = blueprint != null && blueprint.getTaskType() != null ? blueprint.getTaskType().name() : "CONCEPT_EXPLAIN";
        TaskExecutionRecord record = TaskExecutionRecord.builder()
                .taskId(taskId)
                .taskType(taskTypeName)
                .completionStatus(request.getCompletionStatus() != null ? request.getCompletionStatus().name() : TaskCompletionStatus.COMPLETED.name())
                .durationMinutes(request.getDurationMinutes())
                .interactionCount(request.getInteractionCount())
                .userSummarySubmitted(Boolean.TRUE.equals(request.getUserSummarySubmitted()))
                .microPracticeResult(request.getMicroPracticeResult())
                .detectedIssueTags(request.getDetectedIssueTags())
                .behaviorSignals(request.getBehaviorSignals())
                .learnerReflection(request.getLearnerReflection())
                .build();
        store.getOrCreateTaskRecords(request.getSessionId()).add(record);
        int nextIndex = state.getCurrentTaskIndex() + 1;
        state.setCurrentTaskIndex(nextIndex);
        boolean nextAvailable = nextIndex < state.getTaskSequence().size();
        String nextTaskId = nextAvailable ? state.getTaskSequence().get(nextIndex) : null;
        if (!nextAvailable) {
            state.setStatus(LearningSessionStatus.COMPLETED.name());
        }
        int completed = store.getOrCreateTaskRecords(request.getSessionId()).size();
        CompleteTaskData.SessionProgressItem progress = CompleteTaskData.SessionProgressItem.builder()
                .completedTasks(completed)
                .totalTasks(state.getTaskSequence().size())
                .build();
        return CompleteTaskData.builder()
                .taskExecutionRecord(record)
                .nextTaskAvailable(nextAvailable)
                .nextTaskId(nextTaskId)
                .sessionProgress(progress)
                .build();
    }

    private TaskBlueprint resolveBlueprint(String planId, String taskId) {
        if (planId != null) {
            LearningPlanPreview plan = store.getPlanPreviews().get(planId);
            if (plan != null && plan.getTasks() != null) {
                return plan.getTasks().stream()
                        .filter(t -> t.getTaskId().equals(taskId))
                        .findFirst()
                        .orElse(null);
            }
        }
        return FixedSampleData.taskBlueprints().stream()
                .filter(t -> t.getTaskId().equals(taskId))
                .findFirst()
                .orElse(null);
    }
}
