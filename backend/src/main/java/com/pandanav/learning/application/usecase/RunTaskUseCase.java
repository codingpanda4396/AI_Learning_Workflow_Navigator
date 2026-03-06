package com.pandanav.learning.application.usecase;

import com.pandanav.learning.api.dto.task.RunTaskResponse;

public interface RunTaskUseCase {

    RunTaskResponse run(Long taskId);
}
