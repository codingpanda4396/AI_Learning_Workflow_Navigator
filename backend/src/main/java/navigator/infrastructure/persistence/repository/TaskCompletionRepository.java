package navigator.infrastructure.persistence.repository;

import navigator.infrastructure.persistence.entity.TaskCompletionEntity;

import java.util.List;

public interface TaskCompletionRepository {

    void save(TaskCompletionEntity entity);

    TaskCompletionEntity findByTaskId(Long taskId);

    /**
     * 按 session 维度查询所有完成记录，用于报告与证据聚合。
     */
    List<TaskCompletionEntity> findBySessionId(Long sessionId);
}

