package navigator.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 单任务学习方法画像；报告可聚合为多任务摘要。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningMethodProfile {
    private String sessionId;
    private String taskId;
    private String questioningQuality;
    private Boolean selfExplanationPerformed;
    private String selfExplanationQuality;
    private Boolean checkPassed;
    private List<String> antiPatternObserved;
    private List<String> positiveSignals;
    private List<String> dominantActionTypes;
    private List<String> nextMethodAdvice;

    /** Sprint 4：0–1，来自规则累计 */
    private Double directAnswerDependencyScore;
    /** 如 "5 phases" */
    private String guidancePhaseCoverage;
    /** 提示负担：OK / HIGH */
    private String hintBurden;
}
