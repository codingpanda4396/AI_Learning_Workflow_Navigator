package navigator.application.task;

import navigator.api.dto.TaskInteractionData;
import navigator.api.dto.TaskInteractionRequest;
import navigator.infrastructure.persistence.entity.TaskInteractionEntity;
import navigator.infrastructure.persistence.repository.SessionTaskRepository;
import navigator.infrastructure.persistence.repository.TaskInteractionRepository;
import navigator.infrastructure.persistence.serde.JsonSerde;
import org.springframework.stereotype.Service;

/**
 * Sprint 3.1: 统一执行期事件摄取入口（兼容旧 /interactions）。
 *
 * 兼容策略：
 * - 对外仍返回 TaskInteractionData（accepted=true）
 * - 对内统一写入 task_interaction（旧表）+ task_message（新表，用于恢复/留痕）
 * - 默认 silent：不触发状态机/导师编排，避免改变旧客户端语义
 */
@Service
public class TaskExecutionEventIngestService {

    private final SessionTaskRepository sessionTaskRepository;
    private final TaskInteractionRepository taskInteractionRepository;
    private final TaskExecutionPersistenceService persistenceService;
    private final JsonSerde jsonSerde;

    public TaskExecutionEventIngestService(SessionTaskRepository sessionTaskRepository,
                                           TaskInteractionRepository taskInteractionRepository,
                                           TaskExecutionPersistenceService persistenceService,
                                           JsonSerde jsonSerde) {
        this.sessionTaskRepository = sessionTaskRepository;
        this.taskInteractionRepository = taskInteractionRepository;
        this.persistenceService = persistenceService;
        this.jsonSerde = jsonSerde;
    }

    public TaskInteractionData ingestLegacyInteraction(String taskCode, TaskInteractionRequest request) {
        if (request == null) {
            return TaskInteractionData.builder().taskId(taskCode).accepted(false).build();
        }
        String sessionKey = request.getSessionId();
        Long sessionDbId = extractNumericId(sessionKey);
        Long taskDbId = resolveSessionTaskId(sessionDbId, taskCode);
        if (taskDbId == null) {
            taskDbId = extractNumericId(taskCode);
        }

        TaskInteractionEntity entity = new TaskInteractionEntity();
        entity.setSessionId(sessionDbId);
        entity.setTaskId(taskDbId);
        entity.setInteractionType(request.getInteractionType() != null ? request.getInteractionType() : "GENERIC");
        entity.setUserInput(request.getContentSummary());
        entity.setAssistantOutputSummary(null);
        entity.setExtractedSignalsJson(jsonSerde.toJson(request.getBehaviorSignals()));
        taskInteractionRepository.save(entity);

        // Also write to the unified message table for recovery (silent, no assistant turn).
        if (sessionKey != null && request.getContentSummary() != null && !request.getContentSummary().isBlank()) {
            persistenceService.appendMessage(
                    sessionKey,
                    taskCode,
                    "USER",
                    request.getContentSummary(),
                    null,
                    null,
                    null,
                    "LEGACY"
            );
        }

        return TaskInteractionData.builder()
                .taskId(taskCode)
                .accepted(true)
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
}

