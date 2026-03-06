package com.pandanav.learning.application.usecase;

import com.pandanav.learning.application.query.GetSessionQuery;
import com.pandanav.learning.domain.model.LearningSession;

import java.util.Optional;

public interface GetSessionUseCase {

    Optional<LearningSession> execute(GetSessionQuery query);
}
