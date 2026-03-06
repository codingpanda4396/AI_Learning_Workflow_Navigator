package com.pandanav.learning.application.service;

import com.pandanav.learning.application.query.GetSessionQuery;
import com.pandanav.learning.application.usecase.GetSessionUseCase;
import com.pandanav.learning.domain.model.LearningSession;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class GetSessionService implements GetSessionUseCase {

    @Override
    public Optional<LearningSession> execute(GetSessionQuery query) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }
}
