package com.pandanav.learning.application.usecase;

import com.pandanav.learning.api.dto.session.CurrentSessionResponse;

public interface GetCurrentSessionUseCase {

    CurrentSessionResponse execute(Long userId);
}
