package navigator.infrastructure.persistence.repository;

import navigator.infrastructure.persistence.entity.TaskInteractionEntity;

public interface TaskInteractionRepository {

    void save(TaskInteractionEntity entity);

    /**
     * 查询某个 session 下最新的一条交互记录，用于报告与 next-action 证据聚合。
     */
    TaskInteractionEntity findLatestBySessionId(Long sessionId);
}

