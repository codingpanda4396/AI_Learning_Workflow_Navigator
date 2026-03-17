package navigator.infrastructure.persistence.repository.impl;

import navigator.infrastructure.persistence.entity.LearningSessionEntity;
import navigator.infrastructure.persistence.mapper.LearningSessionMapper;
import navigator.infrastructure.persistence.repository.LearningSessionRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public class LearningSessionRepositoryImpl implements LearningSessionRepository {

    private final LearningSessionMapper mapper;

    public LearningSessionRepositoryImpl(LearningSessionMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public LearningSessionEntity createInitialSession(Long goalId) {
        if (goalId == null) {
            return null;
        }
        LocalDateTime now = LocalDateTime.now();
        LearningSessionEntity entity = new LearningSessionEntity();
        entity.setGoalId(goalId);
        entity.setDiagnosisSessionId(null);
        entity.setStatus("DIAGNOSIS_READY");
        entity.setCurrentTaskId(null);
        entity.setTotalTaskCount(0);
        entity.setCompletedTaskCount(0);
        entity.setStartedAt(null);
        entity.setCompletedAt(null);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        mapper.insert(entity);
        return entity;
    }

    @Override
    public void updateDiagnosisSessionId(Long sessionId, Long diagnosisSessionId) {
        if (sessionId == null) {
            return;
        }
        LearningSessionEntity entity = mapper.selectById(sessionId);
        if (entity == null) {
            return;
        }
        entity.setDiagnosisSessionId(diagnosisSessionId);
        entity.setUpdatedAt(LocalDateTime.now());
        mapper.updateById(entity);
    }

    @Override
    public void updatePlanId(Long sessionId, Long planId) {
        if (sessionId == null) {
            return;
        }
        LearningSessionEntity entity = mapper.selectById(sessionId);
        if (entity == null) {
            return;
        }
        entity.setPlanId(planId);
        entity.setUpdatedAt(LocalDateTime.now());
        mapper.updateById(entity);
    }

    @Override
    public void markDiagnosisCompleted(Long sessionId) {
        if (sessionId == null) {
            return;
        }
        LearningSessionEntity entity = mapper.selectById(sessionId);
        if (entity == null) {
            return;
        }
        entity.setStatus("DIAGNOSIS_COMPLETED");
        entity.setUpdatedAt(LocalDateTime.now());
        mapper.updateById(entity);
    }

    @Override
    public void markPlanCommitted(Long sessionId, int totalTaskCount) {
        if (sessionId == null) {
            return;
        }
        LearningSessionEntity entity = mapper.selectById(sessionId);
        if (entity == null) {
            return;
        }
        entity.setStatus("IN_PROGRESS");
        entity.setTotalTaskCount(totalTaskCount);
        entity.setCompletedTaskCount(0);
        entity.setUpdatedAt(LocalDateTime.now());
        mapper.updateById(entity);
    }
}

