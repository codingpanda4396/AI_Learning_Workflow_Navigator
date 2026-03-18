package navigator.infrastructure.persistence.repository;

import navigator.infrastructure.persistence.entity.TaskMessageEntity;

import java.util.List;

public interface TaskMessageRepository {
    void save(TaskMessageEntity entity);

    List<TaskMessageEntity> findRecentBySessionKeyAndTaskCode(String sessionKey, String taskCode, int limit);
}

