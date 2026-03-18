package navigator.infrastructure.persistence.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import navigator.infrastructure.persistence.entity.TaskExecutionRuntimeEntity;
import navigator.infrastructure.persistence.mapper.TaskExecutionRuntimeMapper;
import navigator.infrastructure.persistence.repository.TaskExecutionRuntimeRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public class TaskExecutionRuntimeRepositoryImpl implements TaskExecutionRuntimeRepository {

    private final TaskExecutionRuntimeMapper mapper;

    public TaskExecutionRuntimeRepositoryImpl(TaskExecutionRuntimeMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public TaskExecutionRuntimeEntity findBySessionKeyAndTaskCode(String sessionKey, String taskCode) {
        if (sessionKey == null || taskCode == null) {
            return null;
        }
        return mapper.selectOne(new QueryWrapper<TaskExecutionRuntimeEntity>()
                .eq("session_key", sessionKey)
                .eq("task_code", taskCode)
                .last("limit 1"));
    }

    @Override
    public void saveOrUpdate(TaskExecutionRuntimeEntity entity) {
        if (entity == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        if (entity.getId() == null) {
            entity.setId(IdWorker.getId());
            if (entity.getCreatedAt() == null) {
                entity.setCreatedAt(now);
            }
        }
        entity.setUpdatedAt(now);

        TaskExecutionRuntimeEntity existing = findBySessionKeyAndTaskCode(entity.getSessionKey(), entity.getTaskCode());
        if (existing == null) {
            mapper.insert(entity);
            return;
        }
        entity.setId(existing.getId());
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(existing.getCreatedAt());
        }
        mapper.updateById(entity);
    }
}

