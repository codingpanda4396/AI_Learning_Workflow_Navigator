package com.pandanav.learning.application.usecase;

import com.pandanav.learning.application.command.StartSessionCommand;
import com.pandanav.learning.domain.model.LearningSession;

public interface StartSessionUseCase {

    LearningSession execute(StartSessionCommand command);
}
