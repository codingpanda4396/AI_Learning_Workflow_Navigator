package navigator.application.guard;

import navigator.api.BusinessErrorCode;
import navigator.api.BusinessException;
import navigator.api.auth.CurrentUserHolder;
import navigator.domain.enums.DiagnosisSessionStatus;
import navigator.infrastructure.memory.InMemoryStore;
import navigator.infrastructure.persistence.entity.DiagnosisSessionEntity;
import navigator.infrastructure.persistence.entity.LearningPlanEntity;
import navigator.infrastructure.persistence.entity.LearningSessionEntity;
import navigator.infrastructure.persistence.repository.DiagnosisSessionRepository;
import navigator.infrastructure.persistence.repository.LearningGoalRepository;
import navigator.infrastructure.persistence.repository.LearningPlanRepository;
import navigator.infrastructure.persistence.repository.LearningSessionRepository;
import org.springframework.stereotype.Component;

@Component
public class EntityLookupGuard {

    private final InMemoryStore store;
    private final LearningGoalRepository learningGoalRepository;
    private final DiagnosisSessionRepository diagnosisSessionRepository;
    private final LearningSessionRepository learningSessionRepository;
    private final LearningPlanRepository learningPlanRepository;

    public EntityLookupGuard(InMemoryStore store,
                             LearningGoalRepository learningGoalRepository,
                             DiagnosisSessionRepository diagnosisSessionRepository,
                             LearningSessionRepository learningSessionRepository,
                             LearningPlanRepository learningPlanRepository) {
        this.store = store;
        this.learningGoalRepository = learningGoalRepository;
        this.diagnosisSessionRepository = diagnosisSessionRepository;
        this.learningSessionRepository = learningSessionRepository;
        this.learningPlanRepository = learningPlanRepository;
    }

    public void requireGoal(String goalId) {
        Long userId = CurrentUserHolder.require().id();
        Long dbId = extractNumericId(goalId);
        if (dbId != null) {
            if (!learningGoalRepository.existsByIdAndUserId(dbId, userId)) {
                throw new BusinessException(BusinessErrorCode.RESOURCE_NOT_FOUND, "goal not found: " + goalId);
            }
            return;
        }
        if (goalId == null || !store.getGoals().containsKey(goalId)) {
            throw new BusinessException(BusinessErrorCode.RESOURCE_NOT_FOUND, "goal not found: " + goalId);
        }
    }

    public void requireDiagnosisSession(String diagnosisId) {
        Long userId = CurrentUserHolder.require().id();
        Long dbId = extractNumericId(diagnosisId);
        if (dbId != null) {
            DiagnosisSessionEntity entity = diagnosisSessionRepository.findById(dbId);
            if (entity == null || !userId.equals(entity.getUserId())) {
                throw new BusinessException(BusinessErrorCode.RESOURCE_NOT_FOUND, "diagnosis session not found: " + diagnosisId);
            }
            return;
        }
        if (diagnosisId == null || !store.getDiagnosisSessionStatuses().containsKey(diagnosisId)) {
            throw new BusinessException(BusinessErrorCode.RESOURCE_NOT_FOUND, "diagnosis session not found: " + diagnosisId);
        }
    }

    public void requirePlan(String planId) {
        Long userId = CurrentUserHolder.require().id();
        Long dbId = extractNumericId(planId);
        if (dbId != null) {
            LearningPlanEntity entity = learningPlanRepository.findByIdAndUserId(dbId, userId);
            if (entity == null) {
                throw new BusinessException(BusinessErrorCode.RESOURCE_NOT_FOUND, "plan not found: " + planId);
            }
            return;
        }
        if (planId == null || !store.getPlanPreviews().containsKey(planId)) {
            throw new BusinessException(BusinessErrorCode.RESOURCE_NOT_FOUND, "plan not found: " + planId);
        }
    }

    public void requireSession(String sessionId) {
        Long userId = CurrentUserHolder.require().id();
        Long dbId = extractNumericId(sessionId);
        if (dbId != null) {
            LearningSessionEntity entity = learningSessionRepository.findById(dbId);
            if (entity == null || !userId.equals(entity.getUserId())) {
                throw new BusinessException(BusinessErrorCode.RESOURCE_NOT_FOUND, "session not found: " + sessionId);
            }
            return;
        }
        if (sessionId == null || !store.getSessions().containsKey(sessionId)) {
            throw new BusinessException(BusinessErrorCode.RESOURCE_NOT_FOUND, "session not found: " + sessionId);
        }
    }

    public void requireTaskInSession(String sessionId, String taskId) {
        requireSession(sessionId);
        InMemoryStore.LearningSessionState state = store.getSessions().get(sessionId);
        if (state != null && state.getTaskSequence() != null) {
            if (!state.getTaskSequence().contains(taskId)) {
                throw new BusinessException(BusinessErrorCode.RESOURCE_NOT_FOUND, "task not in session: " + taskId);
            }
            return;
        }
        throw new BusinessException(BusinessErrorCode.RESOURCE_NOT_FOUND, "task not in session: " + taskId);
    }

    public DiagnosisSessionStatus getDiagnosisStatus(String diagnosisId) {
        requireDiagnosisSession(diagnosisId);
        String status = store.getDiagnosisSessionStatuses().get(diagnosisId);
        return status != null ? DiagnosisSessionStatus.valueOf(status) : null;
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
        } catch (NumberFormatException ignored) {
            return null;
        }
    }
}
