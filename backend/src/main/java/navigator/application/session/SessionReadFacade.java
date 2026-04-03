package navigator.application.session;

import navigator.domain.enums.PlanStatus;
import navigator.infrastructure.memory.InMemoryStore;
import navigator.infrastructure.persistence.entity.LearningPlanEntity;
import navigator.infrastructure.persistence.entity.LearningSessionEntity;
import navigator.application.support.ExternalIdSupport;
import navigator.infrastructure.persistence.repository.LearningPlanRepository;
import navigator.infrastructure.persistence.repository.LearningSessionRepository;
import org.springframework.stereotype.Component;

/**
 * 统一 InMemory 与 DB 的会话/计划只读查询，供 Guard 与用例层复用。
 */
@Component
public class SessionReadFacade {

    private final InMemoryStore store;
    private final LearningSessionRepository learningSessionRepository;
    private final LearningPlanRepository learningPlanRepository;

    public SessionReadFacade(InMemoryStore store,
                             LearningSessionRepository learningSessionRepository,
                             LearningPlanRepository learningPlanRepository) {
        this.store = store;
        this.learningSessionRepository = learningSessionRepository;
        this.learningPlanRepository = learningPlanRepository;
    }

    public LearningSessionEntity findLearningSessionEntity(String sessionId) {
        Long sessionDbId = ExternalIdSupport.extractNumericId(sessionId);
        return sessionDbId != null ? learningSessionRepository.findById(sessionDbId) : null;
    }

    /**
     * 先查内存 planStatuses，再按 plan_ 数字 id 查库。
     */
    public PlanStatus resolvePlanStatus(String planId) {
        if (planId == null) {
            return null;
        }
        PlanStatus fromMemory = store.getPlanStatuses().get(planId);
        if (fromMemory != null) {
            return fromMemory;
        }
        Long pid = ExternalIdSupport.extractNumericId(planId);
        if (pid == null) {
            return null;
        }
        LearningPlanEntity persistedPlan = learningPlanRepository.findById(pid);
        return persistedPlan != null && persistedPlan.getStatus() != null
                ? PlanStatus.valueOf(persistedPlan.getStatus())
                : null;
    }
}
