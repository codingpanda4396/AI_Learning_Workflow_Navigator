package navigator.application.task;

import navigator.domain.model.*;
import navigator.infrastructure.memory.InMemoryStore;
import org.springframework.stereotype.Component;

/**
 * 执行期上下文装配器：从 store 汇聚 goal、context、profile、strategy、executableTaskSpec。
 */
@Component
public class TaskExecutionContextAssembler {

    private final InMemoryStore store;

    public TaskExecutionContextAssembler(InMemoryStore store) {
        this.store = store;
    }

    public TaskExecutionContext assemble(String sessionId, String taskId) {
        InMemoryStore.LearningSessionState state = store.getSessions().get(sessionId);
        if (state == null) {
            return null;
        }
        String planId = state.getPlanId();
        if (planId == null) {
            return null;
        }
        LearningPlanPreview plan = store.getPlanPreviews().get(planId);
        if (plan == null) {
            return null;
        }
        String goalId = plan.getGoalId();
        String diagnosisId = store.getDiagnosisToSession().entrySet().stream()
                .filter(e -> sessionId.equals(e.getValue()))
                .map(java.util.Map.Entry::getKey)
                .findFirst()
                .orElse(null);

        StructuredLearningGoal goal = goalId != null ? store.getGoals().get(goalId) : null;
        GoalContextSnapshot goalContextSnapshot = goalId != null ? store.getGoalContextSnapshots().get(goalId) : null;
        LearnerProfileSnapshot learnerProfile = diagnosisId != null ? store.getLearnerProfiles().get(diagnosisId) : null;
        LearnerStrategyProfile learnerStrategyProfile = diagnosisId != null ? store.getLearnerStrategyProfiles().get(diagnosisId) : null;
        ExecutableTaskSpec executableTaskSpec = store.getExecutableTaskSpecs().get(InMemoryStore.taskRuntimeKey(sessionId, taskId));

        int taskIndex = state.getTaskSequence() != null ? state.getTaskSequence().indexOf(taskId) : -1;
        int totalTasks = state.getTaskSequence() != null ? state.getTaskSequence().size() : 0;

        return TaskExecutionContext.builder()
                .sessionId(sessionId)
                .goalId(goalId)
                .planId(planId)
                .taskId(taskId)
                .structuredGoal(goal)
                .goalContextSnapshot(goalContextSnapshot)
                .learnerProfileSnapshot(learnerProfile)
                .learnerStrategyProfile(learnerStrategyProfile)
                .executableTaskSpec(executableTaskSpec)
                .taskIndex(taskIndex >= 0 ? taskIndex : null)
                .totalTasks(totalTasks > 0 ? totalTasks : null)
                .build();
    }
}
