package navigator.application;

import navigator.api.dto.CreateGoalData;
import navigator.application.goal.GoalContextDeriver;
import navigator.application.goal.GoalRuleEngine;
import navigator.domain.model.GoalContextSnapshot;
import navigator.domain.model.LearningGoalInput;
import navigator.domain.model.StructuredLearningGoal;
import navigator.infrastructure.memory.InMemoryStore;
import navigator.infrastructure.persistence.entity.LearningGoalEntity;
import navigator.infrastructure.persistence.repository.LearningGoalRepository;
import navigator.infrastructure.persistence.serde.JsonSerde;
import org.springframework.stereotype.Service;

@Service
public class GoalApplicationService {

    private final InMemoryStore store;
    private final GoalRuleEngine goalRuleEngine;
    private final GoalContextDeriver goalContextDeriver;
    private final LearningGoalRepository learningGoalRepository;
    private final JsonSerde jsonSerde;

    public GoalApplicationService(InMemoryStore store,
                                  GoalRuleEngine goalRuleEngine,
                                  GoalContextDeriver goalContextDeriver,
                                  LearningGoalRepository learningGoalRepository,
                                  JsonSerde jsonSerde) {
        this.store = store;
        this.goalRuleEngine = goalRuleEngine;
        this.goalContextDeriver = goalContextDeriver;
        this.learningGoalRepository = learningGoalRepository;
        this.jsonSerde = jsonSerde;
    }

    public CreateGoalData createGoal(LearningGoalInput input) {
        StructuredLearningGoal goal = goalRuleEngine.derive(input);
        GoalContextSnapshot snapshot = goalContextDeriver.derive(goal);
        String goalId = FixedSampleData.GOAL_ID;
        store.getGoals().put(goalId, goal);
        store.getGoalContextSnapshots().put(goalId, snapshot);
        persistGoalToDb(goalId, input, goal, snapshot);
        return CreateGoalData.builder()
                .goalId(goalId)
                .structuredGoal(goal)
                .goalContextSnapshot(snapshot)
                .build();
    }

    private void persistGoalToDb(String goalId,
                                 LearningGoalInput input,
                                 StructuredLearningGoal goal,
                                 GoalContextSnapshot snapshot) {
        Long dbId = extractNumericId(goalId);
        if (dbId == null) {
            return;
        }
        LearningGoalEntity entity = new LearningGoalEntity();
        entity.setId(dbId);
        entity.setRawGoalText(input != null ? input.getRawGoalText() : goal != null ? goal.getRawGoalText() : null);
        entity.setTimeBudget(input != null && input.getTimeBudget() != null ? input.getTimeBudget().name() : null);
        entity.setSelfReportedLevel(input != null && input.getSelfReportedLevel() != null ? input.getSelfReportedLevel().name() : null);
        entity.setPreferenceTagsJson(jsonSerde.toJson(input != null ? input.getPreferenceTags() : null));
        entity.setGoalTypeHint(input != null && input.getGoalTypeHint() != null ? input.getGoalTypeHint().name() : null);
        entity.setSubjectHint(input != null ? input.getSubjectHint() : null);
        entity.setTopicHintsJson(jsonSerde.toJson(input != null ? input.getTopicHints() : null));
        entity.setSourceContext(input != null ? input.getSourceContext() : null);
        entity.setStructuredGoalJson(jsonSerde.toJson(goal));
        entity.setGoalContextJson(jsonSerde.toJson(snapshot));
        entity.setCreatedAt(java.time.LocalDateTime.now());
        entity.setUpdatedAt(entity.getCreatedAt());
        learningGoalRepository.saveNew(entity);
    }

    private Long extractNumericId(String id) {
        if (id == null) {
            return null;
        }
        String digits = id.replaceAll("\\D+", "");
        if (digits.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(digits);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
