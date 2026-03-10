package com.pandanav.learning.application.usecase;

import com.pandanav.learning.api.dto.session.PlanSessionResponse;
import com.pandanav.learning.domain.enums.PlanMode;

public interface PlanSessionTasksUseCase {

    default PlanSessionResponse execute(Long sessionId) {
        return execute(sessionId, PlanMode.AUTO);
    }

    PlanSessionResponse execute(Long sessionId, PlanMode mode);
}
