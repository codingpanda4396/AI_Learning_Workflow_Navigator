package navigator.application.task;

import navigator.api.dto.CompleteTaskRequest;
import org.springframework.stereotype.Component;

/**
 * 任务完成前的收束校验占位。当前产品策略：不做 summary / 框架要点等强校验，
 * 用户从反思阶段点击收口即可进入报告（由 {@link navigator.application.task.TaskCompletionApplicationService#completeTask} 将会话置为完成）。
 */
@Component
public class TaskClosureValidator {

    public void validateIfRuntimeTracked(String sessionId, String taskId, CompleteTaskRequest request) {
        // no-op
    }
}
