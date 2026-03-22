package navigator.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import navigator.domain.model.TaskExecutionRecord;

/**
 * POST /api/tasks/{taskId}/complete 响应。
 * 合同冻结：taskExecutionRecord, nextTaskAvailable, nextTaskId, sessionProgress 为稳定字段。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompleteTaskData {
    private TaskExecutionRecord taskExecutionRecord;
    private boolean nextTaskAvailable;
    private String nextTaskId;
    private SessionProgressItem sessionProgress;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SessionProgressItem {
        private int completedTasks;
        private int totalTasks;
    }
}
