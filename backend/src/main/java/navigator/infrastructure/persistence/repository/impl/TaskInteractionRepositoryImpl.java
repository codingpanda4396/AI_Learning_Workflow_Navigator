package navigator.infrastructure.persistence.repository.impl;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import navigator.infrastructure.persistence.entity.TaskInteractionEntity;
import navigator.infrastructure.persistence.mapper.TaskInteractionMapper;
import navigator.infrastructure.persistence.repository.TaskInteractionRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public class TaskInteractionRepositoryImpl implements TaskInteractionRepository {

    private final TaskInteractionMapper mapper;

    public TaskInteractionRepositoryImpl(TaskInteractionMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void save(TaskInteractionEntity entity) {
        if (entity == null) {
            return;
        }
        if (entity.getId() == null) {
            entity.setId(IdWorker.getId());
        }
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(LocalDateTime.now());
        }
        mapper.insert(entity);
    }

    @Override
    public TaskInteractionEntity findLatestBySessionId(Long sessionId) {
        if (sessionId == null) {
            return null;
        }
        return mapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<TaskInteractionEntity>()
                        .eq("session_id", sessionId)
                        .orderByDesc("created_at")
                        .last("limit 1")
        );
    }
}

