package navigator.infrastructure.persistence.repository;

import navigator.domain.enums.DiagnosisSessionStatus;
import navigator.infrastructure.persistence.entity.DiagnosisSessionEntity;

public interface DiagnosisSessionRepository {

    void saveNew(Long diagnosisSessionId,
                 Long sessionId,
                 Long goalId,
                 String status,
                 String generationMode,
                 String questionsJson);

    DiagnosisSessionEntity findById(Long diagnosisSessionId);

    void markCompleted(Long diagnosisSessionId, DiagnosisSessionStatus expectedCurrentStatus);
}

