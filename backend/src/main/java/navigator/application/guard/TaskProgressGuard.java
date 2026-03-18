package navigator.application.guard;

import navigator.api.BusinessErrorCode;
import navigator.api.BusinessException;
import navigator.application.task.TaskExecutionRuntime;
import navigator.infrastructure.persistence.entity.TaskExecutionRuntimeEntity;
import navigator.infrastructure.persistence.repository.TaskExecutionRuntimeRepository;
import navigator.domain.enums.TaskExecutionState;
import navigator.domain.model.TaskExecutionRecord;
import navigator.infrastructure.memory.InMemoryStore;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Sprint 1: task complete 前置校验——须为当前任务、未已完成、session 未完成。
 */
@Component
public class TaskProgressGuard {

    private final EntityLookupGuard entityLookupGuard;
    private final InMemoryStore store;
    private final TaskExecutionRuntimeRepository taskExecutionRuntimeRepository;

    public TaskProgressGuard(EntityLookupGuard entityLookupGuard,
                             InMemoryStore store,
                             TaskExecutionRuntimeRepository taskExecutionRuntimeRepository) {
        this.entityLookupGuard = entityLookupGuard;
        this.store = store;
        this.taskExecutionRuntimeRepository = taskExecutionRuntimeRepository;
    }

    /**
     * 允许 complete 的条件：session IN_PROGRESS，taskId 为当前任务，该 task 尚未有完成记录。
     */
    public void requireTaskCanComplete(String sessionId, String taskId) {
        entityLookupGuard.requireTaskInSession(sessionId, taskId);
        InMemoryStore.LearningSessionState state = store.getSessions().get(sessionId);
        if ("COMPLETED".equals(state.getStatus())) {
            throw new BusinessException(BusinessErrorCode.SESSION_ALREADY_COMPLETED, "session already completed");
        }
        List<String> seq = state.getTaskSequence();
        int currentIndex = state.getCurrentTaskIndex();
        if (currentIndex >= seq.size() || !seq.get(currentIndex).equals(taskId)) {
            throw new BusinessException(BusinessErrorCode.TASK_NOT_CURRENT, "task is not current task");
        }
        List<TaskExecutionRecord> records = store.getOrCreateTaskRecords(sessionId);
        boolean alreadyCompleted = records.stream().anyMatch(r -> taskId.equals(r.getTaskId()));
        if (alreadyCompleted) {
            throw new BusinessException(BusinessErrorCode.TASK_ALREADY_COMPLETED, "task already completed");
        }
        String rtKey = InMemoryStore.taskRuntimeKey(sessionId, taskId);
        TaskExecutionRuntime rt = store.getTaskExecutionRuntimes().get(rtKey);
        if (rt != null) {
            if (rt.getState() != TaskExecutionState.PASS) {
                throw new BusinessException(BusinessErrorCode.TASK_EXECUTION_NOT_READY_FOR_COMPLETE,
                        "已启用任务脚手架流程，请先完成探索、自我解释与微检查直至通过");
            }
            return;
        }

        // Sprint 3.1: 若内存 runtime 不在（例如重启），优先查持久化 runtime 以避免误放行 complete。
        TaskExecutionRuntimeEntity persisted = taskExecutionRuntimeRepository.findBySessionKeyAndTaskCode(sessionId, taskId);
        if (persisted != null && persisted.getCurrentState() != null && !TaskExecutionState.PASS.name().equals(persisted.getCurrentState())) {
            throw new BusinessException(BusinessErrorCode.TASK_EXECUTION_NOT_READY_FOR_COMPLETE,
                    "已启用任务脚手架流程，请先完成探索、自我解释与微检查直至通过");
        }
    }
}
