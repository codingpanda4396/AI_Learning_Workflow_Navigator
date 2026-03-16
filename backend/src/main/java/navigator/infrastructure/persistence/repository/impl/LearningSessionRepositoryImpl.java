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
    public void createInitialSession(Long sessionId,
                                     Long goalId,
                                     Long diagnosisSessionId) {
        LocalDateTime now = LocalDateTime.now();
        LearningSessionEntity entity = new LearningSessionEntity();
        entity.setId(sessionId);
        entity.setGoalId(goalId);
        entity.setDiagnosisSessionId(diagnosisSessionId);
        entity.setStatus("DIAGNOSIS_READY");
        entity.setCurrentTaskId(null);
        entity.setTotalTaskCount(0);
        entity.setCompletedTaskCount(0);
        entity.setStartedAt(null);
        entity.setCompletedAt(null);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        mapper.insert(entity);
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
}

