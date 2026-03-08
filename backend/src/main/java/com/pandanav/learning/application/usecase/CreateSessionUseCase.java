package com.pandanav.learning.application.usecase;

import com.pandanav.learning.api.dto.session.CreateSessionRequest;
import com.pandanav.learning.api.dto.session.CreateSessionResponse;

public interface CreateSessionUseCase {

    CreateSessionResponse execute(CreateSessionRequest request, Long userId);
}
