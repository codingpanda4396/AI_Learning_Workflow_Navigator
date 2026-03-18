package navigator.infrastructure.persistence.repository.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import navigator.infrastructure.persistence.entity.TaskMethodProfileEntity;
import navigator.infrastructure.persistence.mapper.TaskMethodProfileMapper;
import navigator.infrastructure.persistence.repository.TaskMethodProfileRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public class TaskMethodProfileRepositoryImpl implements TaskMethodProfileRepository {

    private final TaskMethodProfileMapper mapper;

    public TaskMethodProfileRepositoryImpl(TaskMethodProfileMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void save(TaskMethodProfileEntity entity) {
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
    public List<TaskMethodProfileEntity> findBySessionKey(String sessionKey) {
        if (sessionKey == null) {
            return List.of();
        }
        return mapper.selectList(new QueryWrapper<TaskMethodProfileEntity>()
                .eq("session_key", sessionKey)
                .orderByAsc("created_at"));
    }
}

