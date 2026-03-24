package navigator.application.guard;

import navigator.api.BusinessErrorCode;
import navigator.api.BusinessException;
import navigator.domain.enums.DiagnosisSessionStatus;
import navigator.domain.enums.PlanStatus;
import navigator.infrastructure.memory.InMemoryStore;
import org.springframework.stereotype.Component;

/**
 * Sprint 1: 前置状态校验——preview 需诊断完成，current-task 需 plan 已 commit，report 需 session 完成。
 */
@Component
public class SessionStateGuard {

    private final EntityLookupGuard entityLookupGuard;
    private final InMemoryStore store;

    public SessionStateGuard(EntityLookupGuard entityLookupGuard, InMemoryStore store) {
        this.entityLookupGuard = entityLookupGuard;
        this.store = store;
    }

    /** preview 前：诊断必须已完成。 */
    public void requireDiagnosisCompletedForPreview(String diagnosisId) {
        entityLookupGuard.requireDiagnosisSession(diagnosisId);
        DiagnosisSessionStatus status = entityLookupGuard.getDiagnosisStatus(diagnosisId);
        if (status != DiagnosisSessionStatus.COMPLETED) {
            throw new BusinessException(BusinessErrorCode.DIAGNOSIS_NOT_COMPLETED, "diagnosis not completed");
        }
    }

    /** current-task 前：session 必须 IN_PROGRESS 且 plan 已 COMMITTED。 */
    public void requireSessionInProgressWithCommittedPlan(String sessionId) {
        entityLookupGuard.requireSession(sessionId);
        InMemoryStore.LearningSessionState state = store.getSessions().get(sessionId);
        if (state == null) {
            throw new BusinessException(BusinessErrorCode.RESOURCE_NOT_FOUND, "session not found: " + sessionId);
        }
        if (!"IN_PROGRESS".equals(state.getStatus())) {
            throw new BusinessException(BusinessErrorCode.PLAN_NOT_COMMITTED, "plan not committed or session not in progress");
        }
        String planId = state.getPlanId();
        if (planId == null) {
            throw new BusinessException(BusinessErrorCode.PLAN_NOT_COMMITTED, "plan not committed");
        }
        PlanStatus planStatus = store.getPlanStatuses().get(planId);
        if (planStatus != PlanStatus.COMMITTED) {
            throw new BusinessException(BusinessErrorCode.PLAN_NOT_COMMITTED, "plan not committed");
        }
    }

    /** report 前：session 必须 COMPLETED。 */
    public void requireSessionCompletedForReport(String sessionId) {
        entityLookupGuard.requireSession(sessionId);
        InMemoryStore.LearningSessionState state = store.getSessions().get(sessionId);
        if (state == null) {
            throw new BusinessException(BusinessErrorCode.RESOURCE_NOT_FOUND, "session not found: " + sessionId);
        }
        if (!"COMPLETED".equals(state.getStatus())) {
            throw new BusinessException(BusinessErrorCode.SESSION_NOT_COMPLETED, "session not completed");
        }
    }
}
