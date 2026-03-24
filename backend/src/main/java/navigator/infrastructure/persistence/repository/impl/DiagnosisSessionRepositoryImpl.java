package navigator.infrastructure.persistence.repository.impl;

import navigator.domain.enums.DiagnosisSessionStatus;
import navigator.infrastructure.persistence.entity.DiagnosisSessionEntity;
import navigator.infrastructure.persistence.mapper.DiagnosisSessionMapper;
import navigator.infrastructure.persistence.repository.DiagnosisSessionRepository;
import org.springframework.stereotype.Repository;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import java.time.LocalDateTime;

@Repository
public class DiagnosisSessionRepositoryImpl implements DiagnosisSessionRepository {

    private final DiagnosisSessionMapper mapper;

    public DiagnosisSessionRepositoryImpl(DiagnosisSessionMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public DiagnosisSessionEntity saveNew(Long userId,
                                         Long sessionId,
                                         Long goalId,
                                         String status,
                                         String generationMode,
                                         String questionsJson) {
        if (userId == null || sessionId == null || goalId == null) {
            return null;
        }
        LocalDateTime now = LocalDateTime.now();
        DiagnosisSessionEntity entity = new DiagnosisSessionEntity();
        entity.setUserId(userId);
        entity.setSessionId(sessionId);
        entity.setGoalId(goalId);
        entity.setStatus(status != null ? status : DiagnosisSessionStatus.READY.name());
        entity.setGenerationMode(generationMode != null ? generationMode : "STRUCTURED");
        entity.setQuestionsJson(questionsJson);
        entity.setSubmittedAt(null);
        entity.setCompletedAt(null);
        entity.setCreatedAt(now);
        mapper.insert(entity);
        return entity;
    }

    @Override
    public DiagnosisSessionEntity findById(Long diagnosisSessionId) {
        if (diagnosisSessionId == null) {
            return null;
        }
        return mapper.selectById(diagnosisSessionId);
    }

    @Override
    public DiagnosisSessionEntity findBySessionId(Long sessionId) {
        if (sessionId == null) {
            return null;
        }
        return mapper.selectOne(new QueryWrapper<DiagnosisSessionEntity>()
                .eq("session_id", sessionId)
                .orderByDesc("id")
                .last("LIMIT 1"));
    }

    @Override
    public void markCompleted(Long diagnosisSessionId, DiagnosisSessionStatus expectedCurrentStatus) {
        if (diagnosisSessionId == null) {
            return;
        }
        DiagnosisSessionEntity entity = mapper.selectById(diagnosisSessionId);
        if (entity == null) {
            return;
        }
        // 简单 compare-and-update：仅当当前状态仍为期望值时才更新为 COMPLETED
        if (expectedCurrentStatus == null || expectedCurrentStatus.name().equals(entity.getStatus())) {
            entity.setStatus(DiagnosisSessionStatus.COMPLETED.name());
            if (entity.getSubmittedAt() == null) {
                entity.setSubmittedAt(LocalDateTime.now());
            }
            entity.setCompletedAt(LocalDateTime.now());
            mapper.updateById(entity);
        }
    }
}

