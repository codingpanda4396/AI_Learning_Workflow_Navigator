package com.pandanav.learning.application.usecase;

import com.pandanav.learning.application.command.GenerateTasksCommand;
import com.pandanav.learning.domain.model.LearningTask;

import java.util.List;

public interface GenerateTasksUseCase {

    List<LearningTask> execute(GenerateTasksCommand command);
}
