package navigator.infrastructure.persistence.repository;

import navigator.domain.model.DiagnosisEvidenceSummary;
import navigator.domain.model.LearnerProfileSnapshot;

public interface LearnerProfileSnapshotRepository {

    void saveProfile(Long diagnosisSessionId,
                     Long sessionId,
                     LearnerProfileSnapshot profile,
                     DiagnosisEvidenceSummary evidenceSummary);
}

