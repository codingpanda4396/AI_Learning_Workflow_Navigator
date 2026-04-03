package navigator.application.task;

import navigator.api.BusinessErrorCode;
import navigator.api.BusinessException;
import navigator.api.dto.CompleteTaskData;
import navigator.api.dto.CompleteTaskRequest;
import navigator.application.execution.ExecutionSessionStateService;
import navigator.application.guard.TaskProgressGuard;
import navigator.application.support.ExternalIdSupport;
import navigator.application.task.guidance.TaskExecutionEvidenceAccumulator;
import navigator.domain.enums.LearningSessionStatus;
import navigator.domain.enums.TaskCompletionStatus;
import navigator.domain.model.LearningMethodProfile;
import navigator.domain.model.TaskBlueprint;
import navigator.domain.model.TaskExecutionRecord;
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

/**
 * 任务完成流水线：校验、证据聚合、持久化与会话进度推进。
 */
@Service
public class TaskCompletionApplicationService {

    private final InMemoryStore store;
    private final TaskProgressGuard taskProgressGuard;
    private final SessionTaskRepository sessionTaskRepository;
    private final LearningSessionRepository learningSessionRepository;
    private final TaskCompletionRepository taskCompletionRepository;
    private final TaskMethodProfileRepository taskMethodProfileRepository;
    private final TaskExecutionPersistenceService taskExecutionPersistenceService;
    private final JsonSerde jsonSerde;
    private final ExecutionSessionStateService executionSessionStateService;

    public TaskCompletionApplicationService(InMemoryStore store,
                                            TaskProgressGuard taskProgressGuard,
                                            SessionTaskRepository sessionTaskRepository,
                                            LearningSessionRepository learningSessionRepository,
                                            TaskCompletionRepository taskCompletionRepository,
                                            TaskMethodProfileRepository taskMethodProfileRepository,
                                            TaskExecutionPersistenceService taskExecutionPersistenceService,
                                            JsonSerde jsonSerde,
                                            ExecutionSessionStateService executionSessionStateService) {
        this.store = store;
        this.taskProgressGuard = taskProgressGuard;
        this.sessionTaskRepository = sessionTaskRepository;
        this.learningSessionRepository = learningSessionRepository;
        this.taskCompletionRepository = taskCompletionRepository;
        this.taskMethodProfileRepository = taskMethodProfileRepository;
        this.taskExecutionPersistenceService = taskExecutionPersistenceService;
        this.jsonSerde = jsonSerde;
        this.executionSessionStateService = executionSessionStateService;
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
        InMemoryStore.LearningSessionState state = executionSessionStateService.loadOrHydrateLearningSession(sessionId);
        if (state == null) {
            throw new BusinessException(BusinessErrorCode.RESOURCE_NOT_FOUND,
                    "session not found: " + sessionId);
        }
        TaskBlueprint blueprint = executionSessionStateService.resolveBlueprint(sessionId, state.getPlanId(), taskId);
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
        Long sessionDbId = ExternalIdSupport.extractNumericId(sessionId);
        Long taskDbId = resolveSessionTaskId(sessionDbId, taskId);
        if (taskDbId == null) {
            taskDbId = ExternalIdSupport.extractNumericId(taskId);
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
        int totalTasks = state.getTaskSequence() != null ? state.getTaskSequence().size() : 0;
        int nextIndex = state.getCurrentTaskIndex() + 1;
        state.setCurrentTaskIndex(Math.max(0, Math.min(nextIndex, totalTasks)));

        boolean reflectionClosureRequest = isReflectionClosureRequest(request);
        boolean nextAvailable = !reflectionClosureRequest && nextIndex < totalTasks;
        String nextTaskId = nextAvailable ? state.getTaskSequence().get(nextIndex) : null;

        if (reflectionClosureRequest || !nextAvailable) {
            state.setStatus(LearningSessionStatus.COMPLETED.name());
        }
        int completed = store.getOrCreateTaskRecords(request.getSessionId()).size();
        String persistedStatus = reflectionClosureRequest || !nextAvailable
                ? LearningSessionStatus.COMPLETED.name()
                : LearningSessionStatus.IN_PROGRESS.name();
        learningSessionRepository.updateProgress(
                ExternalIdSupport.extractNumericId(sessionId),
                completed,
                persistedStatus
        );
        CompleteTaskData.SessionProgressItem progress = CompleteTaskData.SessionProgressItem.builder()
                .completedTasks(completed)
                .totalTasks(totalTasks)
                .build();
        return CompleteTaskData.builder()
                .taskExecutionRecord(record)
                .nextTaskAvailable(nextAvailable)
                .nextTaskId(nextTaskId)
                .sessionProgress(progress)
                .sessionStatus(reflectionClosureRequest || !nextAvailable
                        ? LearningSessionStatus.REPORT_READY.name()
                        : LearningSessionStatus.TASK_ACTIVE.name())
                .currentPhase(reflectionClosureRequest || !nextAvailable ? "report" : "task")
                .nextRoute(reflectionClosureRequest || !nextAvailable ? "/report" : "/tasks/" + nextTaskId + "/run")
                .reportReady(reflectionClosureRequest || !nextAvailable)
                .build();
    }

    private boolean isReflectionClosureRequest(CompleteTaskRequest request) {
        return request != null
                && request.getClosurePayloadVersion() != null
                && !request.getClosurePayloadVersion().isBlank();
    }

    private Long resolveSessionTaskId(Long sessionDbId, String taskCode) {
        if (sessionDbId == null || taskCode == null) {
            return null;
        }
        var sessionTask = sessionTaskRepository.findBySessionIdAndTaskCode(sessionDbId, taskCode);
        return sessionTask != null ? sessionTask.getId() : null;
    }
}
