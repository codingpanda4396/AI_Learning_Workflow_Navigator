package com.pandanav.learning.application.service;

import com.pandanav.learning.domain.enums.PlanInstanceStatus;
import com.pandanav.learning.domain.model.PlanInstance;
import com.pandanav.learning.domain.repository.PlanInstanceRepository;
import com.pandanav.learning.domain.repository.SessionRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service
public class PlanInstanceService {

    private final PlanInstanceRepository planInstanceRepository;
    private final SessionRepository sessionRepository;

    public PlanInstanceService(
        PlanInstanceRepository planInstanceRepository,
        SessionRepository sessionRepository
    ) {
        this.planInstanceRepository = planInstanceRepository;
        this.sessionRepository = sessionRepository;
    }

    public PlanInstance ensureActiveForSession(Long sessionId, Long sourcePlanId) {
        PlanInstance existing = planInstanceRepository.findActiveBySessionId(sessionId).orElse(null);
        if (existing != null) {
            bindToSession(sessionId, existing.getId());
            return existing;
        }

        PlanInstance created = new PlanInstance();
        created.setSessionId(sessionId);
        created.setSourcePlanId(sourcePlanId);
        created.setStatus(PlanInstanceStatus.ACTIVE);

        try {
            PlanInstance saved = planInstanceRepository.save(created);
            bindToSession(sessionId, saved.getId());
            return saved;
        } catch (DuplicateKeyException ex) {
            PlanInstance conflictRead = planInstanceRepository.findActiveBySessionId(sessionId)
                .orElseThrow(() -> ex);
            bindToSession(sessionId, conflictRead.getId());
            return conflictRead;
        }
    }

    private void bindToSession(Long sessionId, Long planInstanceId) {
        if (planInstanceId != null) {
            sessionRepository.updateCurrentPlanInstance(sessionId, planInstanceId);
        }
    }
}
