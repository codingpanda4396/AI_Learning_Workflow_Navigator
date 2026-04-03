package navigator.application.guard;

import navigator.api.BusinessErrorCode;
import navigator.api.BusinessException;
import navigator.domain.enums.DiagnosisSessionStatus;
import navigator.domain.enums.LearningSessionStatusSupport;
import navigator.domain.enums.PlanStatus;
import navigator.application.session.SessionReadFacade;
import navigator.infrastructure.memory.InMemoryStore;
import navigator.infrastructure.persistence.entity.LearningPlanEntity;
import navigator.infrastructure.persistence.entity.LearningSessionEntity;
import navigator.infrastructure.persistence.repository.LearningPlanRepository;
import org.springframework.stereotype.Component;

/**
 * Sprint 1: 前置状态校验——preview 需诊断完成，current-task 需 plan 已 commit，report 需 session 完成。
 */
@Component
public class SessionStateGuard {

    private final EntityLookupGuard entityLookupGuard;
    private final InMemoryStore store;
    private final SessionReadFacade sessionReadFacade;
    private final LearningPlanRepository learningPlanRepository;

    public SessionStateGuard(EntityLookupGuard entityLookupGuard,
                             InMemoryStore store,
                             SessionReadFacade sessionReadFacade,
                             LearningPlanRepository learningPlanRepository) {
        this.entityLookupGuard = entityLookupGuard;
        this.store = store;
        this.sessionReadFacade = sessionReadFacade;
        this.learningPlanRepository = learningPlanRepository;
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
        if (state != null) {
            if (LearningSessionStatusSupport.isReportReady(state.getStatus(), state.getCurrentTaskIndex(),
                    state.getTaskSequence() != null ? state.getTaskSequence().size() : 0)) {
                throw new BusinessException(BusinessErrorCode.SESSION_ALREADY_COMPLETED, "session already completed");
            }
            String planId = state.getPlanId();
            if (planId == null) {
                throw new BusinessException(BusinessErrorCode.PLAN_NOT_COMMITTED, "plan not committed");
            }
            PlanStatus planStatus = sessionReadFacade.resolvePlanStatus(planId);
            if (planStatus != PlanStatus.COMMITTED) {
                throw new BusinessException(BusinessErrorCode.PLAN_NOT_COMMITTED, "plan not committed");
            }
            return;
        }

        LearningSessionEntity session = sessionReadFacade.findLearningSessionEntity(sessionId);
        if (session == null) {
            throw new BusinessException(BusinessErrorCode.RESOURCE_NOT_FOUND, "session not found: " + sessionId);
        }
        if (LearningSessionStatusSupport.isReportReady(session.getStatus(),
                session.getCompletedTaskCount() != null ? session.getCompletedTaskCount() : 0,
                session.getTotalTaskCount() != null ? session.getTotalTaskCount() : 0)) {
            throw new BusinessException(BusinessErrorCode.SESSION_ALREADY_COMPLETED, "session already completed");
        }
        if (session.getPlanId() == null) {
            throw new BusinessException(BusinessErrorCode.PLAN_NOT_COMMITTED, "plan not committed");
        }
        LearningPlanEntity persistedPlan = learningPlanRepository.findById(session.getPlanId());
        if (persistedPlan == null || !PlanStatus.COMMITTED.name().equals(persistedPlan.getStatus())) {
            throw new BusinessException(BusinessErrorCode.PLAN_NOT_COMMITTED, "plan not committed");
        }
    }

    /** report 前：session 必须 COMPLETED。 */
    public void requireSessionCompletedForReport(String sessionId) {
        entityLookupGuard.requireSession(sessionId);
        InMemoryStore.LearningSessionState state = store.getSessions().get(sessionId);
        if (state != null) {
            if (!LearningSessionStatusSupport.isReportReady(state.getStatus(), state.getCurrentTaskIndex(),
                    state.getTaskSequence() != null ? state.getTaskSequence().size() : 0)) {
                throw new BusinessException(BusinessErrorCode.SESSION_NOT_COMPLETED, "session not completed");
            }
            return;
        }
        LearningSessionEntity session = sessionReadFacade.findLearningSessionEntity(sessionId);
        if (session == null) {
            throw new BusinessException(BusinessErrorCode.RESOURCE_NOT_FOUND, "session not found: " + sessionId);
        }
        if (!LearningSessionStatusSupport.isReportReady(session.getStatus(),
                session.getCompletedTaskCount() != null ? session.getCompletedTaskCount() : 0,
                session.getTotalTaskCount() != null ? session.getTotalTaskCount() : 0)) {
            throw new BusinessException(BusinessErrorCode.SESSION_NOT_COMPLETED, "session not completed");
        }
    }
}
