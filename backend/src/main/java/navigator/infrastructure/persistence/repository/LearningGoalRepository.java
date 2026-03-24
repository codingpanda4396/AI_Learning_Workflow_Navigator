package navigator.infrastructure.persistence.repository;

import navigator.infrastructure.persistence.entity.LearningGoalEntity;

public interface LearningGoalRepository {

    void saveNew(LearningGoalEntity entity);

    LearningGoalEntity findById(Long id);

    LearningGoalEntity findByIdAndUserId(Long id, Long userId);

    boolean exists(Long id);

    boolean existsByIdAndUserId(Long id, Long userId);
}

