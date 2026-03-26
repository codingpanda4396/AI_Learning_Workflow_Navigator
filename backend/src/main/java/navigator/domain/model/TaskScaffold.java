package navigator.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskScaffold {
    private String scaffoldId;
    private String taskId;
    private String sessionId;
    private String taskType;
    /**
     * 任务级学习意图（认知层一句话），区别于 {@link TaskBlueprint#getGoal()} 的执行描述。
     */
    private String taskLevelLearningIntent;
    private String learningObjective;
    private String whyThisTask;
    /** 认知推进单元（理解 → 探索 → 自解释 → 校验）；可为空由工厂补全。 */
    private List<CognitiveUnit> cognitiveUnits;
    private List<String> recommendedAskTemplates;
    private List<String> recommendedFollowupTemplates;
    private List<String> selfCheckTemplates;
    private List<String> fallbackHints;
    private List<String> completionSignals;
    private List<String> antiPatterns;
    private String currentExecutionState;
    private Integer suggestedExploreTurns;
    private Integer suggestedCheckpointCount;
    private LocalDateTime createdAt;
    /** 学习脚手架引擎（动作卡、校验状态）；可选，向后兼容。 */
    private LearningScaffoldEngineState learningScaffoldEngineState;
}
