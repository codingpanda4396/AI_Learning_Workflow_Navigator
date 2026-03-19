package navigator.application.planning;

import navigator.domain.enums.EntryGranularity;
import navigator.domain.enums.TimeBudget;
import navigator.domain.model.DiagnosisEvidenceSummary;
import navigator.domain.model.GoalContextSnapshot;
import navigator.domain.model.LearnerProfileSnapshot;
import navigator.domain.model.LearnerStrategyProfile;
import navigator.domain.model.StructuredLearningGoal;
import navigator.domain.model.TimeBudgetConstraint;
import navigator.infrastructure.memory.InMemoryStore;
import org.springframework.stereotype.Component;

/**
 * Sprint 1: 从 store 按 goalId、diagnosisId 组装 PlanningContext。
 */
@Component
public class PlanningContextAssembler {

    private final InMemoryStore store;

    public PlanningContextAssembler(InMemoryStore store) {
        this.store = store;
    }

    public PlanningContext assemble(String goalId, String diagnosisId) {
        StructuredLearningGoal goal = store.getGoals().get(goalId);
        GoalContextSnapshot goalContextSnapshot = store.getGoalContextSnapshots().get(goalId);
        LearnerProfileSnapshot learnerProfileSnapshot = store.getLearnerProfiles().get(diagnosisId);
        LearnerStrategyProfile learnerStrategyProfile = store.getLearnerStrategyProfiles().get(diagnosisId);
        DiagnosisEvidenceSummary diagnosisEvidenceSummary = store.getDiagnosisEvidenceSummaries().get(diagnosisId);
        TimeBudgetConstraint timeBudgetConstraint = deriveTimeBudgetConstraint(goal, goalContextSnapshot);
        return PlanningContext.builder()
                .goal(goal)
                .goalContextSnapshot(goalContextSnapshot)
                .learnerProfileSnapshot(learnerProfileSnapshot)
                .learnerStrategyProfile(learnerStrategyProfile)
                .diagnosisEvidenceSummary(diagnosisEvidenceSummary)
                .timeBudgetConstraint(timeBudgetConstraint)
                .build();
    }

    private TimeBudgetConstraint deriveTimeBudgetConstraint(StructuredLearningGoal goal, GoalContextSnapshot goalContextSnapshot) {
        TimeBudget budget = goal != null && goal.getTimeBudget() != null ? goal.getTimeBudget() : TimeBudget.WITHIN_60_MIN;
        int totalCap = totalMinutesCapFor(budget);
        EntryGranularity granularity = goalContextSnapshot != null && goalContextSnapshot.getEntryGranularity() != null
                ? goalContextSnapshot.getEntryGranularity() : EntryGranularity.SMALL;
        int minTasks = 1;
        int maxTasks = maxTasksFor(budget, granularity);
        return TimeBudgetConstraint.builder()
                .timeBudget(budget)
                .totalMinutesCap(totalCap)
                .minTasks(minTasks)
                .maxTasks(maxTasks)
                .build();
    }

    private int totalMinutesCapFor(TimeBudget budget) {
        return switch (budget) {
            case WITHIN_15_MIN -> 15;
            case WITHIN_30_MIN -> 30;
            case WITHIN_60_MIN -> 60;
            case MULTI_DAY, LONG_TERM -> -1;
            default -> 60;
        };
    }

    private int maxTasksFor(TimeBudget budget, EntryGranularity granularity) {
        if (budget == TimeBudget.WITHIN_15_MIN) {
            return granularity == EntryGranularity.MICRO ? 2 : 2;
        }
        if (budget == TimeBudget.WITHIN_30_MIN) {
            return granularity == EntryGranularity.MICRO ? 2 : 3;
        }
        if (budget == TimeBudget.WITHIN_60_MIN) {
            return 5;
        }
        return 8;
    }
}
