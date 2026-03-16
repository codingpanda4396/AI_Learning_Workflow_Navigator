package navigator.application.planning;

import navigator.domain.model.DiagnosisEvidenceSummary;
import navigator.domain.model.GoalContextSnapshot;
import navigator.domain.model.LearnerProfileSnapshot;
import navigator.domain.model.StructuredLearningGoal;
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
        DiagnosisEvidenceSummary diagnosisEvidenceSummary = store.getDiagnosisEvidenceSummaries().get(diagnosisId);
        return PlanningContext.builder()
                .goal(goal)
                .goalContextSnapshot(goalContextSnapshot)
                .learnerProfileSnapshot(learnerProfileSnapshot)
                .diagnosisEvidenceSummary(diagnosisEvidenceSummary)
                .build();
    }
}
