package com.pandanav.learning.application.service;

import com.pandanav.learning.application.command.GenerateTasksCommand;
import com.pandanav.learning.application.usecase.GenerateTasksUseCase;
import com.pandanav.learning.domain.model.LearningTask;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GenerateTasksService implements GenerateTasksUseCase {

    @Override
    public List<LearningTask> execute(GenerateTasksCommand command) {
        throw new UnsupportedOperationException("Not implemented yet.");
    }
}
