package navigator.application.guard;

import navigator.api.BusinessErrorCode;
import navigator.api.BusinessException;
import navigator.domain.enums.DiagnosisSessionStatus;
import navigator.domain.enums.PlanStatus;
import navigator.infrastructure.memory.InMemoryStore;
import navigator.infrastructure.persistence.entity.LearningPlanEntity;
import navigator.infrastructure.persistence.entity.LearningSessionEntity;
import navigator.infrastructure.persistence.repository.LearningPlanRepository;
import navigator.infrastructure.persistence.repository.LearningSessionRepository;
import org.springframework.stereotype.Component;

/**
 * Sprint 1: 前置状态校验——preview 需诊断完成，current-task 需 plan 已 commit，report 需 session 完成。
 */
@Component
public class SessionStateGuard {

    private final EntityLookupGuard entityLookupGuard;
    private final InMemoryStore store;
    private final LearningSessionRepository learningSessionRepository;
    private final LearningPlanRepository learningPlanRepository;

    public SessionStateGuard(EntityLookupGuard entityLookupGuard,
                             InMemoryStore store,
                             LearningSessionRepository learningSessionRepository,
                             LearningPlanRepository learningPlanRepository) {
        this.entityLookupGuard = entityLookupGuard;
        this.store = store;
        this.learningSessionRepository = learningSessionRepository;
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
            if (isReportReady(state.getStatus(), state.getCurrentTaskIndex(),
                    state.getTaskSequence() != null ? state.getTaskSequence().size() : 0)) {
                throw new BusinessException(BusinessErrorCode.SESSION_ALREADY_COMPLETED, "session already completed");
            }
            String planId = state.getPlanId();
            if (planId == null) {
                throw new BusinessException(BusinessErrorCode.PLAN_NOT_COMMITTED, "plan not committed");
            }
            PlanStatus planStatus = store.getPlanStatuses().get(planId);
            if (planStatus == null && extractNumericId(planId) != null) {
                LearningPlanEntity persistedPlan = learningPlanRepository.findById(extractNumericId(planId));
                planStatus = persistedPlan != null && persistedPlan.getStatus() != null
                        ? PlanStatus.valueOf(persistedPlan.getStatus())
                        : null;
            }
            if (planStatus != PlanStatus.COMMITTED) {
                throw new BusinessException(BusinessErrorCode.PLAN_NOT_COMMITTED, "plan not committed");
            }
            return;
        }

        LearningSessionEntity session = loadSessionEntity(sessionId);
        if (session == null) {
            throw new BusinessException(BusinessErrorCode.RESOURCE_NOT_FOUND, "session not found: " + sessionId);
        }
        if (isReportReady(session.getStatus(),
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
            if (!isReportReady(state.getStatus(), state.getCurrentTaskIndex(),
                    state.getTaskSequence() != null ? state.getTaskSequence().size() : 0)) {
                throw new BusinessException(BusinessErrorCode.SESSION_NOT_COMPLETED, "session not completed");
            }
            return;
        }
        LearningSessionEntity session = loadSessionEntity(sessionId);
        if (session == null) {
            throw new BusinessException(BusinessErrorCode.RESOURCE_NOT_FOUND, "session not found: " + sessionId);
        }
        if (!isReportReady(session.getStatus(),
                session.getCompletedTaskCount() != null ? session.getCompletedTaskCount() : 0,
                session.getTotalTaskCount() != null ? session.getTotalTaskCount() : 0)) {
            throw new BusinessException(BusinessErrorCode.SESSION_NOT_COMPLETED, "session not completed");
        }
    }

    private boolean isReportReady(String rawStatus, int completedTaskCount, int totalTaskCount) {
        return "COMPLETED".equals(rawStatus)
                || "REPORT_READY".equals(rawStatus)
                || (totalTaskCount > 0 && completedTaskCount >= totalTaskCount);
    }

    private LearningSessionEntity loadSessionEntity(String sessionId) {
        Long sessionDbId = extractNumericId(sessionId);
        return sessionDbId != null ? learningSessionRepository.findById(sessionDbId) : null;
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
