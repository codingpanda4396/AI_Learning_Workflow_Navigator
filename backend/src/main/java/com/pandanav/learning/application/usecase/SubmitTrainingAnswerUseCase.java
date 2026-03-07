package com.pandanav.learning.application.usecase;

import com.pandanav.learning.api.dto.task.SubmitTaskRequest;
import com.pandanav.learning.api.dto.task.SubmitTaskResponse;

public interface SubmitTrainingAnswerUseCase {

    SubmitTaskResponse submit(Long taskId, SubmitTaskRequest request);
}


