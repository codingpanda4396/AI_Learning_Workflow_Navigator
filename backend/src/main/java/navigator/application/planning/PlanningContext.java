package navigator.application.planning;

import navigator.domain.model.DiagnosisEvidenceSummary;
import navigator.domain.model.GoalContextSnapshot;
import navigator.domain.model.LearnerProfileSnapshot;
import navigator.domain.model.LearnerStrategyProfile;
import navigator.domain.model.StructuredLearningGoal;
import navigator.domain.model.TimeBudgetConstraint;

import lombok.Builder;
import lombok.Data;

/**
 * Sprint 1: 内部规划上下文，不暴露到 API。
 */
@Data
@Builder
public class PlanningContext {
    private StructuredLearningGoal goal;
    private GoalContextSnapshot goalContextSnapshot;
    private LearnerProfileSnapshot learnerProfileSnapshot;
    private LearnerStrategyProfile learnerStrategyProfile;
    private DiagnosisEvidenceSummary diagnosisEvidenceSummary;
    private TimeBudgetConstraint timeBudgetConstraint;
}
