package navigator.infrastructure.persistence.repository.impl;

import navigator.infrastructure.persistence.entity.LearningGoalEntity;
import navigator.infrastructure.persistence.mapper.LearningGoalMapper;
import navigator.infrastructure.persistence.repository.LearningGoalRepository;
import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

@Repository
public class LearningGoalRepositoryImpl implements LearningGoalRepository {

    private final LearningGoalMapper mapper;

    public LearningGoalRepositoryImpl(LearningGoalMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void saveNew(LearningGoalEntity entity) {
        if (entity == null) {
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
    public LearningGoalEntity findByIdAndUserId(Long id, Long userId) {
        if (id == null || userId == null) {
            return null;
        }
        return mapper.selectOne(new QueryWrapper<LearningGoalEntity>()
                .eq("id", id)
                .eq("user_id", userId));
    }

    @Override
    public boolean exists(Long id) {
        if (id == null) {
            return false;
        }
        return mapper.selectById(id) != null;
    }

    @Override
    public boolean existsByIdAndUserId(Long id, Long userId) {
        return findByIdAndUserId(id, userId) != null;
    }
}

