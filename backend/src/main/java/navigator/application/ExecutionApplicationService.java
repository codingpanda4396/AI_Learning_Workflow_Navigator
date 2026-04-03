package navigator.application;

import navigator.api.dto.CompleteTaskData;
import navigator.api.dto.CompleteTaskRequest;
import navigator.api.dto.CurrentTaskData;
import navigator.api.dto.CurrentTaskGuidanceData;
import navigator.api.dto.TaskInteractionData;
import navigator.application.guard.SessionStateGuard;
import navigator.application.guard.TaskProgressGuard;
import navigator.application.task.TaskExecutionEventIngestService;
import navigator.domain.enums.LearningSessionStatus;
import navigator.domain.enums.TaskCompletionStatus;
import navigator.domain.model.ExecutableTaskSpec;
import navigator.domain.model.LearningPlanPreview;
import navigator.application.task.LearningMethodProfileAggregator;
import navigator.application.scaffold.LearningScaffoldEngineService;
import navigator.application.task.TaskExecutionFlowService;
import navigator.application.task.TaskExecutionPersistenceService;
import navigator.application.task.TaskExecutionRuntime;
import navigator.application.task.guidance.TaskExecutionEvidenceAccumulator;
import navigator.domain.model.LearningMethodProfile;
import navigator.domain.model.TaskBlueprint;
import navigator.domain.model.TaskExecutionRecord;
import navigator.domain.model.ExecutableTaskSpec;
import navigator.infrastructure.memory.InMemoryStore;
import navigator.infrastructure.persistence.entity.TaskCompletionEntity;
import navigator.infrastructure.persistence.entity.TaskMethodProfileEntity;
import navigator.infrastructure.persistence.repository.LearningSessionRepository;
import navigator.infrastructure.persistence.repository.SessionTaskRepository;
import navigator.infrastructure.persistence.repository.TaskCompletionRepository;
import navigator.infrastructure.persistence.repository.TaskMethodProfileRepository;
import navigator.infrastructure.persistence.serde.JsonSerde;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ExecutionApplicationService {

    private final InMemoryStore store;
    private final SessionStateGuard sessionStateGuard;
    private final TaskProgressGuard taskProgressGuard;
    private final SessionTaskRepository sessionTaskRepository;
    private final LearningSessionRepository learningSessionRepository;
    private final TaskCompletionRepository taskCompletionRepository;
    private final TaskMethodProfileRepository taskMethodProfileRepository;
    private final TaskExecutionPersistenceService taskExecutionPersistenceService;
    private final TaskExecutionEventIngestService taskExecutionEventIngestService;
    private final JsonSerde jsonSerde;
    private final TaskExecutionFlowService taskExecutionFlowService;
    private final LearningScaffoldEngineService learningScaffoldEngineService;

    public ExecutionApplicationService(InMemoryStore store,
                                       SessionStateGuard sessionStateGuard,
                                       TaskProgressGuard taskProgressGuard,
                                       SessionTaskRepository sessionTaskRepository,
                                       LearningSessionRepository learningSessionRepository,
                                       TaskCompletionRepository taskCompletionRepository,
                                       TaskMethodProfileRepository taskMethodProfileRepository,
                                       TaskExecutionPersistenceService taskExecutionPersistenceService,
                                       TaskExecutionEventIngestService taskExecutionEventIngestService,
                                       JsonSerde jsonSerde,
                                       TaskExecutionFlowService taskExecutionFlowService,
                                       LearningScaffoldEngineService learningScaffoldEngineService) {
        this.store = store;
        this.sessionStateGuard = sessionStateGuard;
        this.taskProgressGuard = taskProgressGuard;
        this.sessionTaskRepository = sessionTaskRepository;
        this.learningSessionRepository = learningSessionRepository;
        this.taskCompletionRepository = taskCompletionRepository;
        this.taskMethodProfileRepository = taskMethodProfileRepository;
        this.taskExecutionPersistenceService = taskExecutionPersistenceService;
        this.taskExecutionEventIngestService = taskExecutionEventIngestService;
        this.jsonSerde = jsonSerde;
        this.taskExecutionFlowService = taskExecutionFlowService;
        this.learningScaffoldEngineService = learningScaffoldEngineService;
    }

    public CurrentTaskGuidanceData getCurrentTaskGuidance(String sessionId) {
        return taskExecutionFlowService.getCurrentTaskGuidance(sessionId);
    }

    public CurrentTaskData getCurrentTask(String sessionId) {
        sessionStateGuard.requireSessionInProgressWithCommittedPlan(sessionId);
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
        TaskBlueprint blueprint = resolveBlueprint(state.getPlanId(), taskId);
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

    public CompleteTaskData completeTask(String taskId, CompleteTaskRequest request) {
        String sessionId = request.getSessionId();
        taskProgressGuard.requireTaskCanComplete(sessionId, taskId);
        String rtKey = InMemoryStore.taskRuntimeKey(sessionId, taskId);
        TaskExecutionRuntime rtMerge = store.getTaskExecutionRuntimes().get(rtKey);
        if (rtMerge == null) {
            rtMerge = taskExecutionPersistenceService.loadRuntime(sessionId, taskId);
        }
        if (rtMerge != null) {
            request.setEvidenceSnapshot(TaskExecutionEvidenceAccumulator.copySnapshot(rtMerge));
        }
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
        if (rtMerge != null) {
            LearningMethodProfile profile = LearningMethodProfileAggregator.aggregate(sessionId, taskId, rtMerge);
            store.getSessionMethodProfiles().computeIfAbsent(sessionId, k -> new ArrayList<>()).add(profile);
            TaskMethodProfileEntity e = new TaskMethodProfileEntity();
            e.setSessionKey(sessionId);
            e.setTaskCode(taskId);
            e.setProfileJson(jsonSerde.toJson(profile));
            e.setCreatedAt(java.time.LocalDateTime.now());
            taskMethodProfileRepository.save(e);
        }
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
        learningSessionRepository.updateProgress(
                extractNumericId(sessionId),
                completed,
                nextAvailable ? LearningSessionStatus.IN_PROGRESS.name() : LearningSessionStatus.COMPLETED.name()
        );
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

    private static String rubricSummary(ExecutableTaskSpec.EvaluationRubric rubric) {
        if (rubric == null || rubric.getDimensions() == null) return null;
        String dims = rubric.getDimensions().entrySet().stream()
                .map(e -> e.getKey() + ":" + e.getValue())
                .collect(java.util.stream.Collectors.joining("; "));
        return (dims.isEmpty() ? "" : dims)
                + (rubric.getPassThreshold() != null ? " (通过:" + rubric.getPassThreshold() + ")" : "");
    }

    private static String scaffoldPolicySummary(ExecutableTaskSpec.ScaffoldPolicy policy) {
        if (policy == null) return null;
        return "探索轮次≤" + (policy.getMaxExploreTurns() != null ? policy.getMaxExploreTurns() : 3)
                + ", 补救轮次≤" + (policy.getMaxRemedialTurns() != null ? policy.getMaxRemedialTurns() : 2);
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
