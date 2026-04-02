package navigator.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * GET /api/sessions/{sessionId}/current-task 响应。
 * 仅任务元信息与阶段进度摘要，不含脚手架正文与题目数据。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CurrentTaskData {
    private String sessionId;
    private String taskId;
    /** 通常为 packId（与知识点包一致） */
    private String knowledge;
    /** 脚手架引擎当前阶段；未启用引擎时为 null */
    private String currentStage;
    /** STRUCTURE / UNDERSTANDING / TRAINING / REFLECTION 是否已完成 */
    private Map<String, Boolean> progressMap;
    /** 会话内任务序号（第几个 / 共几个） */
    private ProgressItem progress;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProgressItem {
        private int currentIndex;
        private int totalTasks;
    }
}
