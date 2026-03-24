package navigator.infrastructure.persistence.repository;

import navigator.domain.enums.PlanStatus;
import navigator.infrastructure.persistence.entity.LearningPlanEntity;

public interface LearningPlanRepository {

    LearningPlanEntity savePreview(LearningPlanEntity entity);

    LearningPlanEntity findById(Long id);

    LearningPlanEntity findByIdAndUserId(Long id, Long userId);

    /**
     * 按 session 维度获取当前 plan（一个 session 只维护一条当前 plan 记录）。
     */
    LearningPlanEntity findBySessionId(Long sessionId);

    boolean compareAndCommit(Long planId, PlanStatus expectedStatus);
}

