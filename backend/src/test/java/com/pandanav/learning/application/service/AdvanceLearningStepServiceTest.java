package com.pandanav.learning.application.service;

import com.pandanav.learning.api.dto.task.AdvanceStepRequest;
import com.pandanav.learning.api.dto.task.AdvanceStepResponse;
import com.pandanav.learning.domain.enums.LearningStepStatus;
import com.pandanav.learning.domain.enums.Stage;
import com.pandanav.learning.domain.model.CompletionRule;
import com.pandanav.learning.domain.model.LearningStep;
import com.pandanav.learning.domain.model.Task;
import com.pandanav.learning.domain.repository.LearningStepRepository;
import com.pandanav.learning.domain.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdvanceLearningStepServiceTest {

    @Mock
    private TaskRepository taskRepository;
    @Mock
    private LearningStepRepository learningStepRepository;

    @Test
    void shouldMarkDoneAndActivateNextTodoStep() {
        AdvanceLearningStepService service = new AdvanceLearningStepService(taskRepository, learningStepRepository);
        when(taskRepository.findById(1003L)).thenReturn(Optional.of(trainingTask(1003L)));

        LearningStep current = step(9001L, 1003L, 1, LearningStepStatus.ACTIVE);
        LearningStep next = step(9002L, 1003L, 2, LearningStepStatus.TODO);
        when(learningStepRepository.findByIdAndTaskId(9001L, 1003L)).thenReturn(Optional.of(current));
        when(learningStepRepository.findByTaskIdOrderByStepOrder(1003L)).thenReturn(List.of(current, next));

        AdvanceStepResponse response = service.advance(
            1003L,
            9001L,
            new AdvanceStepRequest("DONE", 1, List.of())
        );

        assertEquals(1003L, response.taskId());
        assertNotNull(response.currentStep());
        assertEquals("DONE", response.currentStep().status());
        assertNotNull(response.nextStep());
        assertEquals("ACTIVE", response.nextStep().status());
        verify(learningStepRepository).updateStatus(9001L, LearningStepStatus.DONE);
        verify(learningStepRepository).updateStatus(9002L, LearningStepStatus.ACTIVE);
    }

    private Task trainingTask(Long taskId) {
        Task task = new Task();
        task.setId(taskId);
        task.setStage(Stage.TRAINING);
        return task;
    }

    private LearningStep step(Long stepId, Long taskId, int order, LearningStepStatus status) {
        LearningStep step = new LearningStep();
        step.setId(stepId);
        step.setTaskId(taskId);
        step.setStage(Stage.TRAINING);
        step.setType("QUESTION");
        step.setStepOrder(order);
        step.setStatus(status);
        step.setObjective("step " + order);
        step.setCompletionRule(new CompletionRule("MANUAL_CONFIRM", 1, List.of(), 3));
        return step;
    }
}

