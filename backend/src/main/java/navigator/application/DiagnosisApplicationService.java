package navigator.application;

import navigator.api.dto.DiagnosisSessionData;
import navigator.api.dto.SubmitDiagnosisData;
import navigator.application.diagnosis.DiagnosisAnswerNormalizer;
import navigator.application.diagnosis.DiagnosisEvidenceBuilder;
import navigator.application.diagnosis.DiagnosisRuleEngine;
import navigator.application.guard.EntityLookupGuard;
import navigator.domain.enums.DiagnosisSessionStatus;
import navigator.domain.model.DiagnosisQuestion;
import navigator.domain.model.DiagnosisSubmission;
import navigator.domain.model.GoalContextSnapshot;
import navigator.domain.model.StructuredLearningGoal;
import navigator.infrastructure.memory.InMemoryStore;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DiagnosisApplicationService {

    private final InMemoryStore store;
    private final EntityLookupGuard entityLookupGuard;
    private final DiagnosisRuleEngine diagnosisRuleEngine;
    private final DiagnosisEvidenceBuilder diagnosisEvidenceBuilder;

    public DiagnosisApplicationService(InMemoryStore store, EntityLookupGuard entityLookupGuard,
                                      DiagnosisRuleEngine diagnosisRuleEngine, DiagnosisEvidenceBuilder diagnosisEvidenceBuilder) {
        this.store = store;
        this.entityLookupGuard = entityLookupGuard;
        this.diagnosisRuleEngine = diagnosisRuleEngine;
        this.diagnosisEvidenceBuilder = diagnosisEvidenceBuilder;
    }

    public DiagnosisSessionData createSession(String goalId) {
        entityLookupGuard.requireGoal(goalId);
        List<DiagnosisQuestion> questions = FixedSampleData.diagnosisQuestions();
        String diagnosisId = FixedSampleData.DIAGNOSIS_ID;
        store.getDiagnosisSessionStatuses().put(diagnosisId, DiagnosisSessionStatus.READY.name());
        store.getDiagnosisToGoal().put(diagnosisId, goalId);
        return DiagnosisSessionData.builder()
                .diagnosisId(diagnosisId)
                .sessionId(FixedSampleData.SESSION_ID)
                .status(DiagnosisSessionStatus.READY.name())
                .generationMode("STRUCTURED")
                .questions(questions)
                .build();
    }

    public SubmitDiagnosisData submit(DiagnosisSubmission submission) {
        String diagnosisId = submission.getDiagnosisId();
        entityLookupGuard.requireDiagnosisSession(diagnosisId);
        String status = store.getDiagnosisSessionStatuses().get(diagnosisId);
        if (!DiagnosisSessionStatus.READY.name().equals(status)) {
            throw new navigator.api.BusinessException(navigator.api.BusinessErrorCode.DIAGNOSIS_ALREADY_COMPLETED, "diagnosis already completed");
        }
        DiagnosisAnswerNormalizer.NormalizedAnswers normalized = DiagnosisAnswerNormalizer.normalize(submission.getAnswers());
        String goalId = store.getDiagnosisToGoal().get(diagnosisId);
        StructuredLearningGoal goal = goalId != null ? store.getGoals().get(goalId) : null;
        GoalContextSnapshot goalContext = goalId != null ? store.getGoalContextSnapshots().get(goalId) : null;
        var profile = diagnosisRuleEngine.buildProfile(diagnosisId, normalized, goal, goalContext);
        String primaryGap = profile.getBlockerTags() != null && !profile.getBlockerTags().isEmpty()
                ? profile.getBlockerTags().get(0) : null;
        var evidence = diagnosisEvidenceBuilder.build(profile, goal, goalContext, primaryGap);
        store.getLearnerProfiles().put(diagnosisId, profile);
        store.getDiagnosisEvidenceSummaries().put(diagnosisId, evidence);
        store.getDiagnosisSessionStatuses().put(diagnosisId, DiagnosisSessionStatus.COMPLETED.name());
        return SubmitDiagnosisData.builder()
                .diagnosisId(diagnosisId)
                .learnerProfileSnapshot(profile)
                .diagnosisEvidenceSummary(evidence)
                .build();
    }
}
