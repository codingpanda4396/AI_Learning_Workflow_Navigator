package com.pandanav.learning.application.service;

import com.pandanav.learning.api.dto.task.SubmitTaskRequest;
import com.pandanav.learning.api.dto.task.SubmitTaskResponse;
import com.pandanav.learning.application.usecase.SubmitTrainingAnswerUseCase;
import org.springframework.stereotype.Service;

@Service
public class TaskApplicationService {

    private final SubmitTrainingAnswerUseCase submitTrainingAnswerUseCase;

    public TaskApplicationService(SubmitTrainingAnswerUseCase submitTrainingAnswerUseCase) {
        this.submitTrainingAnswerUseCase = submitTrainingAnswerUseCase;
    }

    public SubmitTaskResponse submitTask(Long taskId, SubmitTaskRequest request) {
        return submitTrainingAnswerUseCase.submit(taskId, request);
    }
}
