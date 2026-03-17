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
import navigator.infrastructure.persistence.entity.SessionTaskEntity;
import navigator.infrastructure.persistence.entity.TaskCompletionEntity;
import navigator.infrastructure.persistence.entity.TaskInteractionEntity;
import navigator.infrastructure.persistence.repository.LearningSessionRepository;
import navigator.infrastructure.persistence.repository.SessionTaskRepository;
import navigator.infrastructure.persistence.repository.TaskCompletionRepository;
import navigator.infrastructure.persistence.repository.TaskInteractionRepository;
import navigator.infrastructure.persistence.serde.JsonSerde;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ExecutionApplicationService {

    private final InMemoryStore store;
    private final SessionStateGuard sessionStateGuard;
    private final TaskProgressGuard taskProgressGuard;
    private final SessionTaskRepository sessionTaskRepository;
    private final TaskInteractionRepository taskInteractionRepository;
    private final TaskCompletionRepository taskCompletionRepository;
    private final LearningSessionRepository learningSessionRepository;
    private final JsonSerde jsonSerde;

    public ExecutionApplicationService(InMemoryStore store,
                                       SessionStateGuard sessionStateGuard,
                                       TaskProgressGuard taskProgressGuard,
                                       SessionTaskRepository sessionTaskRepository,
                                       TaskInteractionRepository taskInteractionRepository,
                                       TaskCompletionRepository taskCompletionRepository,
                                       LearningSessionRepository learningSessionRepository,
                                       JsonSerde jsonSerde) {
        this.store = store;
        this.sessionStateGuard = sessionStateGuard;
        this.taskProgressGuard = taskProgressGuard;
        this.sessionTaskRepository = sessionTaskRepository;
        this.taskInteractionRepository = taskInteractionRepository;
        this.taskCompletionRepository = taskCompletionRepository;
        this.learningSessionRepository = learningSessionRepository;
        this.jsonSerde = jsonSerde;
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
        String promptTemplate = blueprint.getRecommendedPromptTemplate() != null ? blueprint.getRecommendedPromptTemplate() : blueprint.getPromptScaffold();
        CurrentTaskData.CurrentTaskItem item = CurrentTaskData.CurrentTaskItem.builder()
                .taskId(blueprint.getTaskId())
                .title(blueprint.getTitle())
                .taskType(blueprint.getTaskType().name())
                .goal(blueprint.getGoal())
                .whyThisTask(promptTemplate != null ? promptTemplate : FixedSampleData.whyThisTask(taskId))
                .taskMethod(blueprint.getTaskMethod())
                .recommendedPromptTemplate(promptTemplate)
                .estimatedMinutes(blueprint.getEstimatedMinutes())
                .promptScaffold(blueprint.getPromptScaffold())
                .completionCriteria(blueprint.getCompletionCriteria())
                .selfEvaluationQuestions(blueprint.getSelfEvaluationQuestions())
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
        String sessionId = request.getSessionId();
        Long sessionDbId = extractNumericId(sessionId);
        Long taskDbId = resolveSessionTaskId(sessionDbId, taskId);
        if (taskDbId == null) {
            taskDbId = extractNumericId(taskId);
        }
        TaskInteractionEntity entity = new TaskInteractionEntity();
        entity.setSessionId(sessionDbId);
        entity.setTaskId(taskDbId);
        entity.setInteractionType(request.getInteractionType() != null ? request.getInteractionType() : "GENERIC");
        entity.setUserInput(request.getContentSummary());
        entity.setAssistantOutputSummary(null);
        entity.setExtractedSignalsJson(jsonSerde.toJson(request.getBehaviorSignals()));
        taskInteractionRepository.save(entity);
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
        Long sessionDbId = extractNumericId(sessionId);
        Long taskDbId = resolveSessionTaskId(sessionDbId, taskId);
        if (taskDbId == null) {
            taskDbId = extractNumericId(taskId);
        }
        TaskCompletionEntity completionEntity = new TaskCompletionEntity();
        completionEntity.setSessionId(sessionDbId);
        completionEntity.setTaskId(taskDbId);
        completionEntity.setCompletionInputJson(jsonSerde.toJson(request));
        completionEntity.setCompletionStatus(record.getCompletionStatus());
        completionEntity.setQualityLevel(null);
        completionEntity.setDetectedGapTagsJson(jsonSerde.toJson(record.getDetectedIssueTags()));
        completionEntity.setRiskTagsJson(null);
        completionEntity.setNextActionHintsJson(null);
        taskCompletionRepository.save(completionEntity);
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

    private Long resolveSessionTaskId(Long sessionDbId, String taskCode) {
        if (sessionDbId == null || taskCode == null) {
            return null;
        }
        var sessionTask = sessionTaskRepository.findBySessionIdAndTaskCode(sessionDbId, taskCode);
        return sessionTask != null ? sessionTask.getId() : null;
    }

    private Long extractNumericId(String id) {
        if (id == null) {
            return null;
        }
        String digits = id.replaceAll("\\D+", "");
        if (digits.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(digits);
        } catch (NumberFormatException ex) {
            return null;
        }
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
