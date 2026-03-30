package navigator.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import navigator.api.dto.scaffold.ReflectionSummary;

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
    private String knowledgeKey;
    private String packId;
    private String knowledgeType;
    private String scaffoldType;
    private List<String> starterPrompts;
    private String checkpointMode;
    private String visualHintType;
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
    /** Phase 4：脚手架反思沉淀（DFS/BFS 等启用引擎时可选） */
    private ReflectionSummary reflectionSummary;
    /** 单任务工作台：阶段进度 */
    private WorkbenchPhaseProgress phaseProgress;
    /** 单任务工作台：当前任务主卡 */
    private CurrentTaskCard currentTaskCard;
    /** 单任务工作台：思考支架 */
    private ScaffoldGuide scaffoldGuide;
    /** 单任务工作台：结构化表达区 */
    private ExpressionLayout expressionLayout;
    /** 单任务工作台：反馈板 schema */
    private FeedbackSchema feedbackSchema;
    /** 单任务工作台：底部动作条 */
    private ActionBar actionBar;
    /** 单任务工作台：AI 导师辅助信息 */
    private TutorAssist tutorAssist;

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

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkbenchPhaseProgress {
        private List<String> phases;
        private String currentPhase;
        private double overallRatio;
        private String taskIndexLabel;
        private String stepLabel;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CurrentTaskCard {
        private String phaseCode;
        private String phaseDisplay;
        private String currentAction;
        private String taskTitle;
        private String objective;
        private String whyNow;
        private List<String> outputRequirements;
        private List<String> completionCriteria;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GuideSection {
        private String id;
        private String title;
        private String description;
        private String lightHint;
        private String standardHint;
        private String strongHint;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScaffoldGuide {
        private List<GuideSection> sections;
        private List<String> observationBullets;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExpressionField {
        private String id;
        private String label;
        private String placeholder;
        private boolean multiline;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ExpressionLayout {
        private String helperText;
        private List<ExpressionField> fields;
        private String lowFrictionPrompt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FeedbackSchema {
        private String correctTitle;
        private String missingTitle;
        private String confusedTitle;
        private String nextFixTitle;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActionBar {
        private String hintActionLabel;
        private String submitActionLabel;
        private String nextActionLabel;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TutorAssist {
        private String floatingLabel;
        private String panelTitle;
        private List<String> quickQuestions;
    }
}
