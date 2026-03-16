package navigator.application;

import navigator.api.dto.DiagnosisSessionData;
import navigator.api.dto.SubmitDiagnosisData;
import navigator.domain.enums.DiagnosisSessionStatus;
import navigator.domain.model.DiagnosisQuestion;
import navigator.domain.model.DiagnosisSubmission;
import navigator.infrastructure.memory.InMemoryStore;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DiagnosisApplicationService {

    private final InMemoryStore store;

    public DiagnosisApplicationService(InMemoryStore store) {
        this.store = store;
    }

    public DiagnosisSessionData createSession(String goalId) {
        List<DiagnosisQuestion> questions = FixedSampleData.diagnosisQuestions();
        return DiagnosisSessionData.builder()
                .diagnosisId(FixedSampleData.DIAGNOSIS_ID)
                .sessionId(FixedSampleData.SESSION_ID)
                .status(DiagnosisSessionStatus.READY.name())
                .generationMode("STRUCTURED")
                .questions(questions)
                .build();
    }

    public SubmitDiagnosisData submit(DiagnosisSubmission submission) {
        // Sprint 0: 固定返回示例画像与证据摘要
        var profile = FixedSampleData.learnerProfileSnapshot();
        var evidence = FixedSampleData.diagnosisEvidenceSummary();
        store.getLearnerProfiles().put(submission.getDiagnosisId(), profile);
        store.getDiagnosisEvidenceSummaries().put(submission.getDiagnosisId(), evidence);
        return SubmitDiagnosisData.builder()
                .diagnosisId(submission.getDiagnosisId())
                .learnerProfileSnapshot(profile)
                .diagnosisEvidenceSummary(evidence)
                .build();
    }
}
