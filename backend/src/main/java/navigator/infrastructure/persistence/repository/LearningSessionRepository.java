package navigator.infrastructure.persistence.repository;

public interface LearningSessionRepository {

    void createInitialSession(Long sessionId,
                              Long goalId,
                              Long diagnosisSessionId);

    void markDiagnosisCompleted(Long sessionId);
}

