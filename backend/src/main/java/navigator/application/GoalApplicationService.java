package navigator.application;

import navigator.api.dto.CreateGoalData;
import navigator.domain.model.GoalContextSnapshot;
import navigator.domain.model.LearningGoalInput;
import navigator.domain.model.StructuredLearningGoal;
import navigator.infrastructure.memory.InMemoryStore;
import org.springframework.stereotype.Service;

@Service
public class GoalApplicationService {

    private final InMemoryStore store;

    public GoalApplicationService(InMemoryStore store) {
        this.store = store;
    }

    public CreateGoalData createGoal(LearningGoalInput input) {
        String rawText = input.getRawGoalText() != null ? input.getRawGoalText() : "我想搞懂链表";
        StructuredLearningGoal goal = FixedSampleData.structuredGoal(rawText);
        GoalContextSnapshot snapshot = FixedSampleData.goalContextSnapshot();
        store.getGoals().put(FixedSampleData.GOAL_ID, goal);
        store.getGoalContextSnapshots().put(FixedSampleData.GOAL_ID, snapshot);
        return CreateGoalData.builder()
                .goalId(FixedSampleData.GOAL_ID)
                .structuredGoal(goal)
                .goalContextSnapshot(snapshot)
                .build();
    }
}
