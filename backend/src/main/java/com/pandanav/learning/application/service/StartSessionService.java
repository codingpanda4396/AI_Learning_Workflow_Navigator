package com.pandanav.learning.application.service;

import com.pandanav.learning.application.command.StartSessionCommand;
import com.pandanav.learning.application.usecase.StartSessionUseCase;
import com.pandanav.learning.domain.model.LearningSession;
import org.springframework.stereotype.Service;

@Service
public class StartSessionService implements StartSessionUseCase {

    @Override
    public LearningSession execute(StartSessionCommand command) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }
}
