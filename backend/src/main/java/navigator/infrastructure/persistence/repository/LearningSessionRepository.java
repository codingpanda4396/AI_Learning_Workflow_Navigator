package navigator.infrastructure.persistence.repository;

import navigator.infrastructure.persistence.entity.LearningSessionEntity;

public interface LearningSessionRepository {

    /**
     * Creates a new learning session with DB-generated id. Returns the entity with id populated.
     * Caller should then create diagnosis_session and call updateDiagnosisSessionId.
     */
    LearningSessionEntity createInitialSession(Long userId, Long goalId);

    void updateDiagnosisSessionId(Long sessionId, Long diagnosisSessionId);

    void updatePlanId(Long sessionId, Long planId);

    void markDiagnosisCompleted(Long sessionId);

    void markPlanCommitted(Long sessionId, int totalTaskCount);

    LearningSessionEntity findById(Long sessionId);

    LearningSessionEntity findLatestByUserId(Long userId);

    void updateProgress(Long sessionId, int completedTaskCount, String status);
}

