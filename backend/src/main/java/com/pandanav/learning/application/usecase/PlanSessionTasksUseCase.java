package com.pandanav.learning.application.usecase;

import com.pandanav.learning.api.dto.session.PlanSessionResponse;

public interface PlanSessionTasksUseCase {

    PlanSessionResponse execute(Long sessionId);
}
