package navigator.infrastructure.persistence.repository;

import navigator.infrastructure.persistence.entity.UserSessionEntity;

public interface UserSessionRepository {

    UserSessionEntity save(UserSessionEntity entity);

    UserSessionEntity findActiveByTokenHash(String tokenHash);

    void deleteByTokenHash(String tokenHash);

    void touch(Long sessionId);
}
