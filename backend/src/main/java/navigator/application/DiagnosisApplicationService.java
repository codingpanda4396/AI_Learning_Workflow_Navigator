package navigator.application;

import navigator.api.dto.DiagnosisSessionData;
import navigator.api.dto.SubmitDiagnosisData;
import navigator.application.diagnosis.DiagnosisAnswerNormalizer;
import navigator.application.diagnosis.DiagnosisEvidenceBuilder;
import navigator.application.diagnosis.DiagnosisQuestionBank;
import navigator.application.diagnosis.DiagnosisRuleEngine;
import navigator.application.guard.EntityLookupGuard;
import navigator.domain.enums.DiagnosisSessionStatus;
import navigator.domain.model.DiagnosisQuestion;
import navigator.domain.model.DiagnosisSubmission;
import navigator.domain.model.GoalContextSnapshot;
import navigator.domain.model.LearnerProfileSnapshot;
import navigator.domain.model.StructuredLearningGoal;
import navigator.infrastructure.memory.InMemoryStore;
import navigator.infrastructure.persistence.entity.DiagnosisSessionEntity;
import navigator.infrastructure.persistence.entity.LearningSessionEntity;
import navigator.infrastructure.persistence.repository.DiagnosisAnswerRepository;
import navigator.infrastructure.persistence.repository.DiagnosisSessionRepository;
import navigator.infrastructure.persistence.repository.LearnerProfileSnapshotRepository;
import navigator.infrastructure.persistence.repository.LearningSessionRepository;
import navigator.infrastructure.persistence.serde.JsonSerde;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DiagnosisApplicationService {

    private final InMemoryStore store;
    private final EntityLookupGuard entityLookupGuard;
    private final DiagnosisRuleEngine diagnosisRuleEngine;
    private final DiagnosisEvidenceBuilder diagnosisEvidenceBuilder;
    private final DiagnosisSessionRepository diagnosisSessionRepository;
    private final LearningSessionRepository learningSessionRepository;
    private final DiagnosisAnswerRepository diagnosisAnswerRepository;
    private final LearnerProfileSnapshotRepository learnerProfileSnapshotRepository;
    private final JsonSerde jsonSerde;

    public DiagnosisApplicationService(InMemoryStore store,
                                       EntityLookupGuard entityLookupGuard,
                                       DiagnosisRuleEngine diagnosisRuleEngine,
                                       DiagnosisEvidenceBuilder diagnosisEvidenceBuilder,
                                       DiagnosisSessionRepository diagnosisSessionRepository,
                                       LearningSessionRepository learningSessionRepository,
                                       DiagnosisAnswerRepository diagnosisAnswerRepository,
                                       LearnerProfileSnapshotRepository learnerProfileSnapshotRepository,
                                       JsonSerde jsonSerde) {
        this.store = store;
        this.entityLookupGuard = entityLookupGuard;
        this.diagnosisRuleEngine = diagnosisRuleEngine;
        this.diagnosisEvidenceBuilder = diagnosisEvidenceBuilder;
        this.diagnosisSessionRepository = diagnosisSessionRepository;
        this.learningSessionRepository = learningSessionRepository;
        this.diagnosisAnswerRepository = diagnosisAnswerRepository;
        this.learnerProfileSnapshotRepository = learnerProfileSnapshotRepository;
        this.jsonSerde = jsonSerde;
    }

    public DiagnosisSessionData createSession(String goalId) {
        entityLookupGuard.requireGoal(goalId);
        StructuredLearningGoal goal = store.getGoals().get(goalId);
        GoalContextSnapshot goalContext = store.getGoalContextSnapshots().get(goalId);
        List<DiagnosisQuestion> questions = DiagnosisQuestionBank.fixedSixQuestions(goal, goalContext);
        Long goalDbId = extractNumericId(goalId);
        if (goalDbId == null) {
            throw new navigator.api.BusinessException(navigator.api.BusinessErrorCode.INVALID_ARGUMENT, "invalid goalId: " + goalId);
        }
        LearningSessionEntity sessionEntity = learningSessionRepository.createInitialSession(goalDbId);
        if (sessionEntity == null || sessionEntity.getId() == null) {
            throw new navigator.api.BusinessException(navigator.api.BusinessErrorCode.INTERNAL_ERROR, "failed to create learning session");
        }
        Long sessionDbId = sessionEntity.getId();
        String questionsJson = jsonSerde.toJson(questions);
        DiagnosisSessionEntity diagEntity = diagnosisSessionRepository.saveNew(
                sessionDbId,
                goalDbId,
                DiagnosisSessionStatus.READY.name(),
                "STRUCTURED",
                questionsJson);
        if (diagEntity == null || diagEntity.getId() == null) {
            throw new navigator.api.BusinessException(navigator.api.BusinessErrorCode.INTERNAL_ERROR, "failed to create diagnosis session");
        }
        learningSessionRepository.updateDiagnosisSessionId(sessionDbId, diagEntity.getId());
        String diagnosisId = "diag_" + diagEntity.getId();
        String sessionId = "learn_session_" + sessionDbId;
        store.getDiagnosisSessionStatuses().put(diagnosisId, DiagnosisSessionStatus.READY.name());
        store.getDiagnosisToGoal().put(diagnosisId, goalId);
        store.getDiagnosisToSession().put(diagnosisId, sessionId);
        return DiagnosisSessionData.builder()
                .diagnosisId(diagnosisId)
                .sessionId(sessionId)
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
        // 持久化标准化答案与 LearnerProfile 快照，并推进状态
        persistSubmissionToDb(diagnosisId, submission, profile, evidence);
        return SubmitDiagnosisData.builder()
                .diagnosisId(diagnosisId)
                .learnerProfileSnapshot(profile)
                .diagnosisEvidenceSummary(evidence)
                .build();
    }

    private void persistSubmissionToDb(String diagnosisId,
                                       DiagnosisSubmission submission,
                                       LearnerProfileSnapshot profile,
                                       navigator.domain.model.DiagnosisEvidenceSummary evidence) {
        Long diagnosisDbId = extractNumericId(diagnosisId);
        String sessionIdStr = store.getDiagnosisToSession().get(diagnosisId);
        Long sessionDbId = extractNumericId(sessionIdStr);
        if (diagnosisDbId == null || submission == null) {
            return;
        }
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        // 1) 写入每题答案
        if (submission.getAnswers() != null && !submission.getAnswers().isEmpty()) {
            diagnosisAnswerRepository.saveAnswers(diagnosisDbId, submission.getAnswers());
        }
        // 2) 写入 LearnerProfileSnapshot
        if (profile != null) {
            learnerProfileSnapshotRepository.saveProfile(
                    diagnosisDbId,
                    sessionDbId,
                    profile,
                    evidence
            );
        }
        // 3) 推进 diagnosis_session 与 learning_session 状态
        diagnosisSessionRepository.markCompleted(diagnosisDbId, DiagnosisSessionStatus.READY);
        learningSessionRepository.markDiagnosisCompleted(sessionDbId);
    }

    private navigator.domain.model.GoalContextSnapshot goalContextFromStore(String diagnosisId) {
        String goalId = store.getDiagnosisToGoal().get(diagnosisId);
        if (goalId == null) {
            return null;
        }
        return store.getGoalContextSnapshots().get(goalId);
    }

    private String goalContextPlanningMode(GoalContextSnapshot snapshot) {
        if (snapshot == null || snapshot.getPlanningMode() == null) {
            return null;
        }
        return snapshot.getPlanningMode().name();
    }

    private Long extractNumericId(String id) {
        if (id == null) {
            return null;
        }
        String digits = id.replaceAll("\\D+", "");
        if (digits.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(digits);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
