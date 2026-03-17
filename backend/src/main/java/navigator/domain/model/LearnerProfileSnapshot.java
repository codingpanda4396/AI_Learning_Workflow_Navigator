package navigator.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import navigator.domain.enums.ExecutionStability;
import navigator.domain.enums.FoundationLevel;
import navigator.domain.enums.LearningPreference;
import navigator.domain.enums.TimeBudget;
import navigator.domain.enums.UrgencyLevel;

import java.util.List;

/**
 * 用户画像快照 V2：收敛为 6 个高价值维度 + diagnosisId。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearnerProfileSnapshot {
    private String diagnosisId;
    private FoundationLevel foundationLevel;
    /** 执行稳定性，从 gap + confidence 推导 */
    private ExecutionStability executionStability;
    /** 时间预算等级，从 goal 传入 */
    private TimeBudget timeBudgetLevel;
    /** 学习偏好，从 goal.preferenceTags 推导 */
    private LearningPreference learningPreference;
    /** 主阻塞点（primary blocker），blockerTags 的首个 */
    private String blockingPoint;
    /** 紧迫程度，从 goal 传入 */
    private UrgencyLevel urgencyLevel;
    /** 阻塞标签列表，保留供 PlanStrategySelector 消费 */
    private List<String> blockerTags;
    /** 风险标签列表 */
    private List<String> riskTags;
}
