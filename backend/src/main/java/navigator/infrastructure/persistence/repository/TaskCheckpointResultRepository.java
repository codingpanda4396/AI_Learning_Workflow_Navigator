package navigator.infrastructure.persistence.repository;

import navigator.infrastructure.persistence.entity.TaskCheckpointResultEntity;

public interface TaskCheckpointResultRepository {
    void save(TaskCheckpointResultEntity entity);
}

