package navigator.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import navigator.domain.enums.TimeBudget;

/**
 * 时间预算约束：供规划层约束任务数与总时长。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TimeBudgetConstraint {
    private TimeBudget timeBudget;
    private int totalMinutesCap;
    private int minTasks;
    private int maxTasks;
}
