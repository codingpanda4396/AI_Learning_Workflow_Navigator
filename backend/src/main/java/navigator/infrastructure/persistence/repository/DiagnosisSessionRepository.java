package navigator.infrastructure.persistence.repository;

import navigator.domain.enums.DiagnosisSessionStatus;
import navigator.infrastructure.persistence.entity.DiagnosisSessionEntity;

public interface DiagnosisSessionRepository {

    /**
     * Creates a new diagnosis session with DB-generated id. Returns the entity with id populated.
     */
    DiagnosisSessionEntity saveNew(Long userId,
                                  Long sessionId,
                                  Long goalId,
                                  String status,
                                  String generationMode,
                                  String questionsJson);

    DiagnosisSessionEntity findById(Long diagnosisSessionId);

    DiagnosisSessionEntity findBySessionId(Long sessionId);

    void markCompleted(Long diagnosisSessionId, DiagnosisSessionStatus expectedCurrentStatus);
}

