package navigator.infrastructure.persistence.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import navigator.infrastructure.persistence.entity.TaskMessageEntity;
import navigator.infrastructure.persistence.mapper.TaskMessageMapper;
import navigator.infrastructure.persistence.repository.TaskMessageRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public class TaskMessageRepositoryImpl implements TaskMessageRepository {

    private final TaskMessageMapper mapper;

    public TaskMessageRepositoryImpl(TaskMessageMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void save(TaskMessageEntity entity) {
        if (entity == null) {
            return;
        }
        if (entity.getId() == null) {
            entity.setId(IdWorker.getId());
        }
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(LocalDateTime.now());
        }
        if (entity.getFallbackMode() == null) {
            entity.setFallbackMode("NONE");
        }
        mapper.insert(entity);
    }

    @Override
    public List<TaskMessageEntity> findRecentBySessionKeyAndTaskCode(String sessionKey, String taskCode, int limit) {
        if (sessionKey == null || taskCode == null) {
            return List.of();
        }
        int n = limit <= 0 ? 20 : Math.min(limit, 100);
        return mapper.selectList(new QueryWrapper<TaskMessageEntity>()
                .eq("session_key", sessionKey)
                .eq("task_code", taskCode)
                .orderByDesc("created_at")
                .last("limit " + n));
    }
}

