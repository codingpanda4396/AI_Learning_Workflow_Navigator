package navigator.infrastructure.persistence.repository.impl;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import navigator.infrastructure.persistence.entity.TaskStateTransitionEntity;
import navigator.infrastructure.persistence.mapper.TaskStateTransitionMapper;
import navigator.infrastructure.persistence.repository.TaskStateTransitionRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public class TaskStateTransitionRepositoryImpl implements TaskStateTransitionRepository {

    private final TaskStateTransitionMapper mapper;

    public TaskStateTransitionRepositoryImpl(TaskStateTransitionMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void save(TaskStateTransitionEntity entity) {
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

