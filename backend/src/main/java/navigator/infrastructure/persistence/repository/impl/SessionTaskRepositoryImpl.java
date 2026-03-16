package navigator.infrastructure.persistence.repository.impl;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import navigator.infrastructure.persistence.entity.SessionTaskEntity;
import navigator.infrastructure.persistence.mapper.SessionTaskMapper;
import navigator.infrastructure.persistence.repository.SessionTaskRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public class SessionTaskRepositoryImpl implements SessionTaskRepository {

    private final SessionTaskMapper mapper;

    public SessionTaskRepositoryImpl(SessionTaskMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void saveAll(List<SessionTaskEntity> tasks) {
        if (tasks == null || tasks.isEmpty()) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        for (SessionTaskEntity entity : tasks) {
            if (entity == null) {
                continue;
            }
            if (entity.getId() == null) {
                entity.setId(IdWorker.getId());
            }
            if (entity.getCreatedAt() == null) {
                entity.setCreatedAt(now);
            }
            mapper.insert(entity);
        }
    }

    @Override
    public List<SessionTaskEntity> findBySessionId(Long sessionId) {
        if (sessionId == null) {
            return List.of();
        }
        return mapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<SessionTaskEntity>()
                        .eq("session_id", sessionId)
                        .orderByAsc("order_index")
        );
    }
}

