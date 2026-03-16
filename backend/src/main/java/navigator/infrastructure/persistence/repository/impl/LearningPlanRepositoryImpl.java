package navigator.infrastructure.persistence.repository.impl;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import navigator.domain.enums.PlanStatus;
import navigator.infrastructure.persistence.entity.LearningPlanEntity;
import navigator.infrastructure.persistence.mapper.LearningPlanMapper;
import navigator.infrastructure.persistence.repository.LearningPlanRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public class LearningPlanRepositoryImpl implements LearningPlanRepository {

    private final LearningPlanMapper mapper;

    public LearningPlanRepositoryImpl(LearningPlanMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void savePreview(LearningPlanEntity entity) {
        if (entity == null) {
            return;
        }
        if (entity.getId() == null) {
            entity.setId(IdWorker.getId());
        }
        LocalDateTime now = LocalDateTime.now();
        if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(now);
        }
        entity.setUpdatedAt(now);
        // upsert by id
        if (mapper.selectById(entity.getId()) == null) {
            mapper.insert(entity);
        } else {
            mapper.updateById(entity);
        }
    }

    @Override
    public LearningPlanEntity findById(Long id) {
        if (id == null) {
            return null;
        }
        return mapper.selectById(id);
    }

    @Override
    public LearningPlanEntity findBySessionId(Long sessionId) {
        if (sessionId == null) {
            return null;
        }
        return mapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<LearningPlanEntity>()
                        .eq("session_id", sessionId)
        );
    }

    @Override
    public boolean compareAndCommit(Long planId, PlanStatus expectedStatus) {
        if (planId == null) {
            return false;
        }
        LearningPlanEntity entity = mapper.selectById(planId);
        if (entity == null) {
            return false;
        }
        if (expectedStatus != null && !expectedStatus.name().equals(entity.getStatus())) {
            return false;
        }
        entity.setStatus(PlanStatus.COMMITTED.name());
        entity.setCommittedAt(LocalDateTime.now());
        entity.setUpdatedAt(entity.getCommittedAt());
        mapper.updateById(entity);
        return true;
    }
}

