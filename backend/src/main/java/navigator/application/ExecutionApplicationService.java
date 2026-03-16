package navigator.application;

import navigator.api.dto.CompleteTaskData;
import navigator.api.dto.CurrentTaskData;
import navigator.api.dto.TaskInteractionData;
import navigator.domain.enums.LearningSessionStatus;
import navigator.domain.enums.TaskCompletionStatus;
import navigator.domain.model.TaskBlueprint;
import navigator.domain.model.TaskExecutionRecord;
import navigator.infrastructure.memory.InMemoryStore;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ExecutionApplicationService {

    private final InMemoryStore store;

    public ExecutionApplicationService(InMemoryStore store) {
        this.store = store;
    }

    public CurrentTaskData getCurrentTask(String sessionId) {
        InMemoryStore.LearningSessionState state = store.getSessions().get(sessionId);
        if (state == null || state.getTaskSequence() == null) {
            return null;
        }
        int idx = state.getCurrentTaskIndex();
        List<String> seq = state.getTaskSequence();
        if (idx >= seq.size()) {
            return null;
        }
        String taskId = seq.get(idx);
        TaskBlueprint blueprint = FixedSampleData.taskBlueprints().stream()
                .filter(t -> t.getTaskId().equals(taskId))
                .findFirst()
                .orElse(null);
        if (blueprint == null) return null;
        CurrentTaskData.CurrentTaskItem item = CurrentTaskData.CurrentTaskItem.builder()
                .taskId(blueprint.getTaskId())
                .title(blueprint.getTitle())
                .taskType(blueprint.getTaskType().name())
                .goal(blueprint.getGoal())
                .whyThisTask(FixedSampleData.whyThisTask(taskId))
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
        InMemoryStore.LearningSessionState state = store.getSessions().get(request.getSessionId());
        if (state == null) return null;
        TaskExecutionRecord record = TaskExecutionRecord.builder()
                .taskId(taskId)
                .taskType(state.getTaskSequence().indexOf(taskId) >= 0 ? FixedSampleData.taskBlueprints().stream()
                        .filter(t -> t.getTaskId().equals(taskId))
                        .findFirst()
                        .map(t -> t.getTaskType().name()).orElse("CONCEPT_EXPLAIN") : "CONCEPT_EXPLAIN")
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
}
