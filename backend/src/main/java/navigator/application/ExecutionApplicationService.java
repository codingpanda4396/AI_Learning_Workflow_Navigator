package navigator.application;

import navigator.api.dto.CompleteTaskData;
import navigator.api.dto.CompleteTaskRequest;
import navigator.api.dto.CurrentTaskData;
import navigator.api.dto.CurrentTaskGuidanceData;
import navigator.api.dto.SessionFlowStateData;
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
        InMemoryStore.LearningSessionState state = loadSessionState(sessionId);
        if (state == null || state.getTaskSequence() == null) {
            return null;
        }
        int idx = state.getCurrentTaskIndex();
        List<String> seq = state.getTaskSequence();
        if (idx >= seq.size()) {
            return null;
        }
        String taskId = seq.get(idx);
        TaskBlueprint blueprint = resolveBlueprint(sessionId, state.getPlanId(), taskId);
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
        InMemoryStore.LearningSessionState state = loadSessionState(sessionId);
        Long sessionDbId = extractNumericId(sessionId);
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

        boolean reportReady = isReportReady(rawStatus, completedTasks, totalTasks);
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
        } else if ("DIAGNOSIS_COMPLETED".equals(rawStatus)) {
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
        InMemoryStore.LearningSessionState state = loadSessionState(sessionId);
        if (state == null) {
            throw new navigator.api.BusinessException(navigator.api.BusinessErrorCode.RESOURCE_NOT_FOUND,
                    "session not found: " + sessionId);
        }
        TaskBlueprint blueprint = resolveBlueprint(sessionId, state.getPlanId(), taskId);
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
                extractNumericId(sessionId),
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

    private InMemoryStore.LearningSessionState loadSessionState(String sessionId) {
        InMemoryStore.LearningSessionState state = store.getSessions().get(sessionId);
        if (state != null && state.getTaskSequence() != null && !state.getTaskSequence().isEmpty()) {
            return state;
        }

        Long sessionDbId = extractNumericId(sessionId);
        if (sessionDbId == null) {
            return state;
        }

        var sessionEntity = learningSessionRepository.findById(sessionDbId);
        var sessionTasks = sessionTaskRepository.findBySessionId(sessionDbId);
        if (sessionEntity == null || sessionTasks == null || sessionTasks.isEmpty()) {
            return state;
        }

        InMemoryStore.LearningSessionState hydrated = new InMemoryStore.LearningSessionState();
        hydrated.setSessionId(sessionId);
        hydrated.setPlanId(sessionEntity.getPlanId() != null ? "plan_" + sessionEntity.getPlanId() : null);
        hydrated.setTaskSequence(sessionTasks.stream().map(t -> t.getTaskCode()).toList());
        hydrated.setCurrentTaskIndex(Math.max(0, sessionEntity.getCompletedTaskCount() != null
                ? sessionEntity.getCompletedTaskCount()
                : 0));
        hydrated.setStatus(sessionEntity.getStatus());
        store.getSessions().put(sessionId, hydrated);
        return hydrated;
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

    private TaskBlueprint resolveBlueprint(String sessionId, String planId, String taskId) {
        if (planId != null) {
            LearningPlanPreview plan = store.getPlanPreviews().get(planId);
            if (plan != null && plan.getTasks() != null) {
                return plan.getTasks().stream()
                        .filter(t -> t.getTaskId().equals(taskId))
                        .findFirst()
                        .orElse(null);
            }
        }
        Long sessionDbId = extractNumericId(sessionId);
        if (sessionDbId != null) {
            var sessionTask = sessionTaskRepository.findBySessionIdAndTaskCode(sessionDbId, taskId);
            if (sessionTask != null && sessionTask.getTaskSnapshotJson() != null && !sessionTask.getTaskSnapshotJson().isBlank()) {
                try {
                    TaskBlueprint hydrated = jsonSerde.fromJson(sessionTask.getTaskSnapshotJson(), TaskBlueprint.class);
                    if (hydrated != null) {
                        return hydrated;
                    }
                } catch (Exception ignored) {
                    // Fall through to fixed sample lookup.
                }
            }
        }
        return FixedSampleData.taskBlueprints().stream()
                .filter(t -> t.getTaskId().equals(taskId))
                .findFirst()
                .orElse(null);
    }

    private boolean isReportReady(String rawStatus, int completedTasks, int totalTasks) {
        return LearningSessionStatus.COMPLETED.name().equals(rawStatus)
                || LearningSessionStatus.REPORT_READY.name().equals(rawStatus)
                || (totalTasks > 0 && completedTasks >= totalTasks);
    }
}
