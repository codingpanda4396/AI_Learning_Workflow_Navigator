package navigator.infrastructure.persistence.repository;

import navigator.infrastructure.persistence.entity.LearningGoalEntity;

public interface LearningGoalRepository {

    void saveNew(LearningGoalEntity entity);

    LearningGoalEntity findById(Long id);

    boolean exists(Long id);
}

