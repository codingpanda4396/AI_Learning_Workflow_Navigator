package navigator.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

/**
 * GET /api/tasks/{taskId}/scaffold 响应。
 * 合同冻结：taskId, recommendedAskTemplates, completionSignals, currentExecutionState 为稳定字段。
 * 可选扩展：cognitiveUnits、taskLevelLearningIntent（向后兼容新增）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskScaffoldResponse {
    private String taskId;
    private String taskType;
    /** 任务级认知意图（可选） */
    private String taskLevelLearningIntent;
    private String learningObjective;
    private String whyThisTask;
    /** 结构化认知单元（可选，缺省时前端可回退到扁平模板字段） */
    private List<CognitiveUnitItem> cognitiveUnits;
    private List<String> recommendedAskTemplates;
    private List<String> recommendedFollowupTemplates;
    private List<String> selfCheckTemplates;
    private List<String> fallbackHints;
    private List<String> completionSignals;
    private List<String> antiPatterns;
    private String currentExecutionState;

    /**
     * Sprint 3.1: 执行态恢复所需的最小快照（向后兼容新增字段）。
     */
    private ExecutionSnapshot executionSnapshot;
    /**
     * Sprint 3.1: 最近对话/事件记录（用于刷新恢复 UI）。
     */
    private List<RecentMessageItem> recentMessages;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExecutionSnapshot {
        private String currentState;
        private int exploreTurnCount;
        private String checkpointQuestion;
        private boolean canComplete;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentMessageItem {
        private String role; // USER / ASSISTANT / SYSTEM
        private String content;
        private String detectedAction;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScaffoldPromptItem {
        private String promptId;
        private String prompt;
        private String intent;
        private boolean required;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CognitiveUnitItem {
        private String unitId;
        private int order;
        private String label;
        private String learningObjective;
        private String targetOutcome;
        private String failureSignal;
        private List<String> actionBullets;
        private List<ScaffoldPromptItem> prompts;
    }
}
