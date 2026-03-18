package navigator.infrastructure.persistence.repository;

import navigator.infrastructure.persistence.entity.TaskStateTransitionEntity;

public interface TaskStateTransitionRepository {
    void save(TaskStateTransitionEntity entity);
}

