package com.pandanav.learning.domain.repository;

import com.pandanav.learning.domain.model.PlanInstance;

import java.util.Optional;

public interface PlanInstanceRepository {

    PlanInstance save(PlanInstance planInstance);

    Optional<PlanInstance> findActiveBySessionId(Long sessionId);
}
