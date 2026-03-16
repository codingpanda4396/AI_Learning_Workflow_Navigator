package navigator.application;

import navigator.api.dto.CreateGoalData;
import navigator.application.goal.GoalContextDeriver;
import navigator.application.goal.GoalRuleEngine;
import navigator.domain.model.GoalContextSnapshot;
import navigator.domain.model.LearningGoalInput;
import navigator.domain.model.StructuredLearningGoal;
import navigator.infrastructure.memory.InMemoryStore;
import org.springframework.stereotype.Service;

@Service
public class GoalApplicationService {

    private final InMemoryStore store;
    private final GoalRuleEngine goalRuleEngine;
    private final GoalContextDeriver goalContextDeriver;

    public GoalApplicationService(InMemoryStore store, GoalRuleEngine goalRuleEngine, GoalContextDeriver goalContextDeriver) {
        this.store = store;
        this.goalRuleEngine = goalRuleEngine;
        this.goalContextDeriver = goalContextDeriver;
    }

    public CreateGoalData createGoal(LearningGoalInput input) {
        StructuredLearningGoal goal = goalRuleEngine.derive(input);
        GoalContextSnapshot snapshot = goalContextDeriver.derive(goal);
        String goalId = FixedSampleData.GOAL_ID;
        store.getGoals().put(goalId, goal);
        store.getGoalContextSnapshots().put(goalId, snapshot);
        return CreateGoalData.builder()
                .goalId(goalId)
                .structuredGoal(goal)
                .goalContextSnapshot(snapshot)
                .build();
    }
}
