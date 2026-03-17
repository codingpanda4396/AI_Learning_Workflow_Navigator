package navigator.infrastructure.persistence.repository;

import navigator.infrastructure.persistence.entity.SessionTaskEntity;

import java.util.List;

public interface SessionTaskRepository {

    void saveAll(List<SessionTaskEntity> tasks);

    List<SessionTaskEntity> findBySessionId(Long sessionId);

    SessionTaskEntity findBySessionIdAndTaskCode(Long sessionId, String taskCode);
}

