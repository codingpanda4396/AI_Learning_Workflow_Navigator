package com.pandanav.learning.application.usecase;

import com.pandanav.learning.api.dto.task.AdvanceStepRequest;
import com.pandanav.learning.api.dto.task.AdvanceStepResponse;

public interface AdvanceLearningStepUseCase {

    AdvanceStepResponse advance(Long taskId, Long stepId, AdvanceStepRequest request);
}

