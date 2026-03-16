package navigator.application.guard;

import navigator.api.BusinessErrorCode;
import navigator.api.BusinessException;
import navigator.domain.enums.DiagnosisSessionStatus;
import navigator.infrastructure.memory.InMemoryStore;
import org.springframework.stereotype.Component;

/**
 * Sprint 1: 实体存在性校验，不存在则抛 RESOURCE_NOT_FOUND。
 */
@Component
public class EntityLookupGuard {

    private final InMemoryStore store;

    public EntityLookupGuard(InMemoryStore store) {
        this.store = store;
    }

    public void requireGoal(String goalId) {
        if (goalId == null || !store.getGoals().containsKey(goalId)) {
            throw new BusinessException(BusinessErrorCode.RESOURCE_NOT_FOUND, "goal not found: " + goalId);
        }
    }

    /** 诊断会话已创建（存在状态记录）即视为存在。 */
    public void requireDiagnosisSession(String diagnosisId) {
        if (diagnosisId == null || !store.getDiagnosisSessionStatuses().containsKey(diagnosisId)) {
            throw new BusinessException(BusinessErrorCode.RESOURCE_NOT_FOUND, "diagnosis session not found: " + diagnosisId);
        }
    }

    public void requirePlan(String planId) {
        if (planId == null || !store.getPlanPreviews().containsKey(planId)) {
            throw new BusinessException(BusinessErrorCode.RESOURCE_NOT_FOUND, "plan not found: " + planId);
        }
    }

    public void requireSession(String sessionId) {
        if (sessionId == null || !store.getSessions().containsKey(sessionId)) {
            throw new BusinessException(BusinessErrorCode.RESOURCE_NOT_FOUND, "session not found: " + sessionId);
        }
    }

    /** 要求 session 存在且 taskId 在 session 的 taskSequence 中。 */
    public void requireTaskInSession(String sessionId, String taskId) {
        requireSession(sessionId);
        InMemoryStore.LearningSessionState state = store.getSessions().get(sessionId);
        if (state.getTaskSequence() == null || !state.getTaskSequence().contains(taskId)) {
            throw new BusinessException(BusinessErrorCode.RESOURCE_NOT_FOUND, "task not in session: " + taskId);
        }
    }

    /** 获取诊断会话状态，若不存在则抛 RESOURCE_NOT_FOUND。 */
    public DiagnosisSessionStatus getDiagnosisStatus(String diagnosisId) {
        requireDiagnosisSession(diagnosisId);
        String status = store.getDiagnosisSessionStatuses().get(diagnosisId);
        return status != null ? DiagnosisSessionStatus.valueOf(status) : null;
    }
}
