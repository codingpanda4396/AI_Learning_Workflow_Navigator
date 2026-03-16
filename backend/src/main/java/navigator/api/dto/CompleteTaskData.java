package navigator.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import navigator.domain.model.TaskExecutionRecord;

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
