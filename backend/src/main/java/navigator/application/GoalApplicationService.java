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
        LearningGoalEntity entity = buildEntity(input, goal, snapshot);
        learningGoalRepository.saveNew(entity);
        Long dbId = entity.getId();
        if (dbId == null) {
            return CreateGoalData.builder()
                    .goalId(FixedSampleData.GOAL_ID)
                    .structuredGoal(goal)
                    .goalContextSnapshot(snapshot)
                    .build();
        }
        String goalId = "goal_" + dbId;
        store.getGoals().put(goalId, goal);
        store.getGoalContextSnapshots().put(goalId, snapshot);
        return CreateGoalData.builder()
                .goalId(goalId)
                .structuredGoal(goal)
                .goalContextSnapshot(snapshot)
                .build();
    }

    private LearningGoalEntity buildEntity(LearningGoalInput input,
                                          StructuredLearningGoal goal,
                                          GoalContextSnapshot snapshot) {
        LearningGoalEntity entity = new LearningGoalEntity();
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
        return entity;
    }
}
