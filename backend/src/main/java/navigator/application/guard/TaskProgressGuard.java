package navigator.application.guard;

import navigator.api.BusinessErrorCode;
import navigator.api.BusinessException;
import navigator.domain.model.TaskExecutionRecord;
import navigator.infrastructure.memory.InMemoryStore;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 任务 complete 前置校验：须为当前任务、未已完成、session 未完成。
 * 不再要求任务执行状态为 PASS（脚手架/反思阶段），以便完成收束后可进入报告页。
 */
@Component
public class TaskProgressGuard {

    private final EntityLookupGuard entityLookupGuard;
    private final InMemoryStore store;

    public TaskProgressGuard(EntityLookupGuard entityLookupGuard,
                             InMemoryStore store) {
        this.entityLookupGuard = entityLookupGuard;
        this.store = store;
    }

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
    }
}
