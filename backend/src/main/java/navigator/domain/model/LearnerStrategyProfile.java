package navigator.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import navigator.domain.enums.FeedbackStyle;
import navigator.domain.enums.ScaffoldIntensity;
import navigator.domain.enums.TaskType;

import java.util.List;

/**
 * 学习策略画像：从 LearnerProfileSnapshot + GoalContextSnapshot 派生，供规划与执行共用。
 *
 * 说明：这是“策略倾向”的结构化表达，不承载任务序列等规划结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearnerStrategyProfile {
    private String diagnosisId;
    private List<TaskType> preferredTaskTypes;
    private ScaffoldIntensity scaffoldIntensity;
    private FeedbackStyle feedbackStyle;
    /**
     * 建议的 checkpoint 频率。可按“每 N 轮/每阶段”在执行侧解释。
     */
    private Integer checkpointFrequency;
    private List<String> riskMitigationTags;
}

