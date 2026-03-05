package com.panda.ainavigator.application.service;

import com.panda.ainavigator.api.dto.session.NextTaskResponse;
import com.panda.ainavigator.api.dto.task.FeedbackResponse;
import com.panda.ainavigator.api.dto.task.RunTaskResponse;
import com.panda.ainavigator.api.dto.task.SubmitTaskRequest;
import com.panda.ainavigator.api.dto.task.SubmitTaskResponse;
import com.panda.ainavigator.domain.model.ErrorTag;
import com.panda.ainavigator.domain.model.NextAction;
import com.panda.ainavigator.domain.model.Stage;
import com.panda.ainavigator.domain.model.TaskStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TaskApplicationService {

    private final SessionApplicationService sessionApplicationService;

    public TaskApplicationService(SessionApplicationService sessionApplicationService) {
        this.sessionApplicationService = sessionApplicationService;
    }

    public RunTaskResponse runTask(Long taskId) {
        return new RunTaskResponse(taskId, Stage.UNDERSTANDING, 101L, TaskStatus.SUCCEEDED,
                sessionApplicationService.mockUnderstandingOutput());
    }

    public SubmitTaskResponse submitTask(Long taskId, SubmitTaskRequest request) {
        return new SubmitTaskResponse(
                taskId,
                Stage.TRAINING,
                101L,
                72,
                List.of(ErrorTag.CONCEPT_CONFUSION, ErrorTag.MISSING_STEPS),
                new FeedbackResponse(
                        "Explanation misses replay and half-open connection scenario.",
                        List.of("Add why two-way handshake is unsafe", "Explain ISN(c) and ISN(s) confirmation path")
                ),
                0.55,
                0.05,
                0.60,
                NextAction.INSERT_TRAINING_VARIANTS,
                new NextTaskResponse(2001L, Stage.TRAINING, 101L)
        );
    }
}