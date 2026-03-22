package navigator.application.task;

import navigator.api.BusinessErrorCode;
import navigator.api.BusinessException;
import navigator.api.dto.CompleteTaskRequest;
import navigator.infrastructure.memory.InMemoryStore;
import navigator.infrastructure.persistence.repository.TaskExecutionRuntimeRepository;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 脚手架任务完成前的收束字段校验（与 PASS 门禁叠加）。
 */
@Component
public class TaskClosureValidator {

    private static final int MIN_SUMMARY_LEN = 10;
    private static final int MIN_FRAMEWORK_POINTS = 2;

    private final InMemoryStore store;
    private final TaskExecutionRuntimeRepository taskExecutionRuntimeRepository;

    public TaskClosureValidator(InMemoryStore store,
                                TaskExecutionRuntimeRepository taskExecutionRuntimeRepository) {
        this.store = store;
        this.taskExecutionRuntimeRepository = taskExecutionRuntimeRepository;
    }

    public void validateIfRuntimeTracked(String sessionId, String taskId, CompleteTaskRequest request) {
        if (!isRuntimeTracked(sessionId, taskId)) {
            return;
        }
        if (request == null) {
            throw new BusinessException(BusinessErrorCode.TASK_CLOSURE_INCOMPLETE, "缺少任务收束信息");
        }
        String summary = request.getSummaryText();
        if (summary == null || summary.trim().length() < MIN_SUMMARY_LEN) {
            throw new BusinessException(BusinessErrorCode.TASK_CLOSURE_INCOMPLETE,
                    "请提交至少 " + MIN_SUMMARY_LEN + " 字的任务总结（summaryText）");
        }
        List<String> fw = request.getLearnedFrameworkPoints();
        if (fw == null || fw.stream().filter(s -> s != null && !s.isBlank()).count() < MIN_FRAMEWORK_POINTS) {
            throw new BusinessException(BusinessErrorCode.TASK_CLOSURE_INCOMPLETE,
                    "请列出至少 " + MIN_FRAMEWORK_POINTS + " 条本任务学到的框架要点（learnedFrameworkPoints）");
        }
        String next = request.getNextPracticeIntent();
        if (next == null || next.isBlank()) {
            throw new BusinessException(BusinessErrorCode.TASK_CLOSURE_INCOMPLETE,
                    "请说明下一步准备如何练习（nextPracticeIntent）");
        }
    }

    private boolean isRuntimeTracked(String sessionId, String taskId) {
        String key = InMemoryStore.taskRuntimeKey(sessionId, taskId);
        if (store.getTaskExecutionRuntimes().containsKey(key)) {
            return true;
        }
        return taskExecutionRuntimeRepository.findBySessionKeyAndTaskCode(sessionId, taskId) != null;
    }
}
