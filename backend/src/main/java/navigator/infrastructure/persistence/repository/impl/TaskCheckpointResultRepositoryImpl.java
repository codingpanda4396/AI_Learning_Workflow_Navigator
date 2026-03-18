package navigator.infrastructure.persistence.repository.impl;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import navigator.infrastructure.persistence.entity.TaskCheckpointResultEntity;
import navigator.infrastructure.persistence.mapper.TaskCheckpointResultMapper;
import navigator.infrastructure.persistence.repository.TaskCheckpointResultRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public class TaskCheckpointResultRepositoryImpl implements TaskCheckpointResultRepository {

    private final TaskCheckpointResultMapper mapper;

    public TaskCheckpointResultRepositoryImpl(TaskCheckpointResultMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void save(TaskCheckpointResultEntity entity) {
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
}

