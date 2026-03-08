package com.pandanav.learning.application.usecase;

import com.pandanav.learning.api.dto.session.PathResponse;

public interface GetSessionPathUseCase {

    PathResponse execute(Long sessionId);
}
