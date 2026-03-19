package navigator.application.task;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import navigator.domain.model.ExecutableTaskSpec;
import navigator.domain.model.GoalContextSnapshot;
import navigator.domain.model.LearnerProfileSnapshot;
import navigator.domain.model.LearnerStrategyProfile;
import navigator.domain.model.StructuredLearningGoal;

/**
 * 执行期上下文：把 goal/context/profile/strategy/taskSpec 汇聚成执行侧单一入口。
 *
 * 注意：这是 application 层对象，用于编排与裁剪上下文，不作为主决策对象对外暴露。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskExecutionContext {
    private String sessionId;
    private String goalId;
    private String planId;
    private String taskId;

    private StructuredLearningGoal structuredGoal;
    private GoalContextSnapshot goalContextSnapshot;
    private LearnerProfileSnapshot learnerProfileSnapshot;
    private LearnerStrategyProfile learnerStrategyProfile;

    /** 当前任务的可执行合同 */
    private ExecutableTaskSpec executableTaskSpec;

    /** 轻量会话索引（用于解释/展示） */
    private Integer taskIndex;
    private Integer totalTasks;
}

