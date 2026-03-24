package navigator.infrastructure.persistence.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import navigator.infrastructure.persistence.entity.UserSessionEntity;
import navigator.infrastructure.persistence.mapper.UserSessionMapper;
import navigator.infrastructure.persistence.repository.UserSessionRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public class UserSessionRepositoryImpl implements UserSessionRepository {

    private final UserSessionMapper mapper;

    public UserSessionRepositoryImpl(UserSessionMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public UserSessionEntity save(UserSessionEntity entity) {
        if (entity == null) {
            return null;
        }
        if (entity.getId() == null) {
            mapper.insert(entity);
        } else {
            mapper.updateById(entity);
        }
        return entity;
    }

    @Override
    public UserSessionEntity findActiveByTokenHash(String tokenHash) {
        if (tokenHash == null || tokenHash.isBlank()) {
            return null;
        }
        UserSessionEntity entity = mapper.selectOne(new QueryWrapper<UserSessionEntity>()
                .eq("token_hash", tokenHash));
        if (entity == null || entity.getExpiresAt() == null || entity.getExpiresAt().isBefore(LocalDateTime.now())) {
            return null;
        }
        return entity;
    }

    @Override
    public void deleteByTokenHash(String tokenHash) {
        if (tokenHash == null || tokenHash.isBlank()) {
            return;
        }
        mapper.delete(new QueryWrapper<UserSessionEntity>().eq("token_hash", tokenHash));
    }

    @Override
    public void touch(Long sessionId) {
        if (sessionId == null) {
            return;
        }
        UserSessionEntity entity = mapper.selectById(sessionId);
        if (entity == null) {
            return;
        }
        entity.setLastAccessedAt(LocalDateTime.now());
        mapper.updateById(entity);
    }
}
