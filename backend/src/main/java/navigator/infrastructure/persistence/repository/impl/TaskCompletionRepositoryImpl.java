package navigator.infrastructure.persistence.repository.impl;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import navigator.infrastructure.persistence.entity.TaskCompletionEntity;
import navigator.infrastructure.persistence.mapper.TaskCompletionMapper;
import navigator.infrastructure.persistence.repository.TaskCompletionRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public class TaskCompletionRepositoryImpl implements TaskCompletionRepository {

    private final TaskCompletionMapper mapper;

    public TaskCompletionRepositoryImpl(TaskCompletionMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void save(TaskCompletionEntity entity) {
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
    public TaskCompletionEntity findByTaskId(Long taskId) {
        if (taskId == null) {
            return null;
        }
        return mapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<TaskCompletionEntity>()
                        .eq("task_id", taskId)
        );
    }

    @Override
    public java.util.List<TaskCompletionEntity> findBySessionId(Long sessionId) {
        if (sessionId == null) {
            return java.util.List.of();
        }
        return mapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<TaskCompletionEntity>()
                        .eq("session_id", sessionId)
        );
    }
}

