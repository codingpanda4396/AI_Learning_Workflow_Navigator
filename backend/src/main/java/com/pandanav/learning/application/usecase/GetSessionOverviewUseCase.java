package com.pandanav.learning.application.usecase;

import com.pandanav.learning.api.dto.session.SessionOverviewResponse;

public interface GetSessionOverviewUseCase {

    SessionOverviewResponse execute(Long sessionId);
}
