package com.pandanav.learning.application.service;

import com.pandanav.learning.api.dto.session.NextTaskResponse;
import com.pandanav.learning.api.dto.task.FeedbackResponse;
import com.pandanav.learning.api.dto.task.SubmitTaskRequest;
import com.pandanav.learning.api.dto.task.SubmitTaskResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
public class TaskApplicationService {

    public SubmitTaskResponse submitTask(Long taskId, SubmitTaskRequest request) {
        return new SubmitTaskResponse(
            taskId,
            "TRAINING",
            101L,
            72,
            List.of("CONCEPT_CONFUSION", "MISSING_STEPS"),
            new FeedbackResponse(
                "Explanation is incomplete for replay and sequence synchronization details.",
                List.of(
                    "Add old-SYN replay counterexample.",
                    "Explain ISN(c)/ISN(s) acknowledgement chain step-by-step."
                )
            ),
            new BigDecimal("0.55"),
            new BigDecimal("0.05"),
            new BigDecimal("0.60"),
            "INSERT_TRAINING_VARIANTS",
            new NextTaskResponse(2001L, "TRAINING", 101L)
        );
    }
}
