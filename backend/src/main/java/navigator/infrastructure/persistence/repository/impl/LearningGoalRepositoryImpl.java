package navigator.infrastructure.persistence.repository.impl;

import navigator.infrastructure.persistence.entity.LearningGoalEntity;
import navigator.infrastructure.persistence.mapper.LearningGoalMapper;
import navigator.infrastructure.persistence.repository.LearningGoalRepository;
import org.springframework.stereotype.Repository;

@Repository
public class LearningGoalRepositoryImpl implements LearningGoalRepository {

    private final LearningGoalMapper mapper;

    public LearningGoalRepositoryImpl(LearningGoalMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void saveNew(LearningGoalEntity entity) {
        if (entity == null || entity.getId() == null) {
            return;
        }
        mapper.insert(entity);
    }

    @Override
    public LearningGoalEntity findById(Long id) {
        if (id == null) {
            return null;
        }
        return mapper.selectById(id);
    }

    @Override
    public boolean exists(Long id) {
        if (id == null) {
            return false;
        }
        return mapper.selectById(id) != null;
    }
}

