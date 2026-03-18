package navigator.infrastructure.persistence.repository;

import navigator.infrastructure.persistence.entity.TaskMethodProfileEntity;

import java.util.List;

public interface TaskMethodProfileRepository {
    void save(TaskMethodProfileEntity entity);

    List<TaskMethodProfileEntity> findBySessionKey(String sessionKey);
}

