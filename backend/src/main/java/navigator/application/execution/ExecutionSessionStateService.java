package navigator.application.execution;

import navigator.application.FixedSampleData;
import navigator.application.support.ExternalIdSupport;
import navigator.domain.model.LearningPlanPreview;
import navigator.domain.model.TaskBlueprint;
import navigator.infrastructure.memory.InMemoryStore;
import navigator.infrastructure.persistence.repository.LearningSessionRepository;
import navigator.infrastructure.persistence.repository.SessionTaskRepository;
import navigator.infrastructure.persistence.serde.JsonSerde;
import org.springframework.stereotype.Component;

/**
 * 学习会话在内存中的装载与任务蓝图解析（含从 DB 补水）。
 */
@Component
public class ExecutionSessionStateService {

    private final InMemoryStore store;
    private final LearningSessionRepository learningSessionRepository;
    private final SessionTaskRepository sessionTaskRepository;
    private final JsonSerde jsonSerde;

    public ExecutionSessionStateService(InMemoryStore store,
                                        LearningSessionRepository learningSessionRepository,
                                        SessionTaskRepository sessionTaskRepository,
                                        JsonSerde jsonSerde) {
        this.store = store;
        this.learningSessionRepository = learningSessionRepository;
        this.sessionTaskRepository = sessionTaskRepository;
        this.jsonSerde = jsonSerde;
    }

    public InMemoryStore.LearningSessionState loadOrHydrateLearningSession(String sessionId) {
        InMemoryStore.LearningSessionState state = store.getSessions().get(sessionId);
        if (state != null && state.getTaskSequence() != null && !state.getTaskSequence().isEmpty()) {
            return state;
        }

        Long sessionDbId = ExternalIdSupport.extractNumericId(sessionId);
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

    public TaskBlueprint resolveBlueprint(String sessionId, String planId, String taskId) {
        if (planId != null) {
            LearningPlanPreview plan = store.getPlanPreviews().get(planId);
            if (plan != null && plan.getTasks() != null) {
                return plan.getTasks().stream()
                        .filter(t -> t.getTaskId().equals(taskId))
                        .findFirst()
                        .orElse(null);
            }
        }
        Long sessionDbId = ExternalIdSupport.extractNumericId(sessionId);
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
}
