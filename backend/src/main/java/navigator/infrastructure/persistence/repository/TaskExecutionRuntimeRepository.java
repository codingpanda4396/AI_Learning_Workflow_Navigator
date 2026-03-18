package navigator.infrastructure.persistence.repository;

import navigator.infrastructure.persistence.entity.TaskExecutionRuntimeEntity;

public interface TaskExecutionRuntimeRepository {
    TaskExecutionRuntimeEntity findBySessionKeyAndTaskCode(String sessionKey, String taskCode);

    void saveOrUpdate(TaskExecutionRuntimeEntity entity);
}

