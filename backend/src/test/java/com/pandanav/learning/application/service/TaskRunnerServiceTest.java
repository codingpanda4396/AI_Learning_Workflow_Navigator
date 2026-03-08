package com.pandanav.learning.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pandanav.learning.api.dto.task.RunTaskResponse;
import com.pandanav.learning.domain.llm.StageContentGenerator;
import com.pandanav.learning.domain.enums.Stage;
import com.pandanav.learning.domain.enums.TaskStatus;
import com.pandanav.learning.domain.model.Task;
import com.pandanav.learning.domain.repository.ConceptNodeRepository;
import com.pandanav.learning.domain.repository.LlmCallLogRepository;
import com.pandanav.learning.domain.repository.MasteryRepository;
import com.pandanav.learning.domain.repository.SessionRepository;
import com.pandanav.learning.domain.repository.TaskRepository;
import com.pandanav.learning.infrastructure.config.LlmProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskRunnerServiceTest {

    @Mock
    private TaskRepository taskRepository;
    @Mock
    private SessionRepository sessionRepository;
    @Mock
    private ConceptNodeRepository conceptNodeRepository;
    @Mock
    private MasteryRepository masteryRepository;
    @Mock
    private StageContentGenerator stageContentGenerator;
    @Mock
    private LlmCallLogRepository llmCallLogRepository;

    @Test
    void shouldReturnStoredOutputWithoutCreatingNewAttemptWhenSucceededAndOutputExists() {
        LlmProperties llmProperties = new LlmProperties();
        TaskRunnerService taskRunnerService = new TaskRunnerService(
            taskRepository,
            sessionRepository,
            conceptNodeRepository,
            masteryRepository,
            stageContentGenerator,
            llmCallLogRepository,
            llmProperties,
            new ObjectMapper()
        );
        Task task = new Task();
        task.setId(1002L);
        task.setSessionId(123L);
        task.setNodeId(101L);
        task.setStage(Stage.UNDERSTANDING);
        task.setStatus(TaskStatus.SUCCEEDED);
        task.setOutputJson("{\"cached\":true}");

        when(taskRepository.findById(1002L)).thenReturn(Optional.of(task));

        RunTaskResponse response = taskRunnerService.run(1002L);

        assertEquals(1002L, response.taskId());
        assertEquals("UNDERSTANDING", response.stage());
        assertEquals(101L, response.nodeId());
        assertEquals("SUCCEEDED", response.status());
        assertNotNull(response.output());
        assertEquals(true, response.output().get("cached").asBoolean());

        verify(taskRepository, never()).createRunningAttempt(1002L);
        verify(taskRepository, never()).markAttemptSucceeded(
            org.mockito.ArgumentMatchers.anyLong(),
            org.mockito.ArgumentMatchers.anyString(),
            org.mockito.ArgumentMatchers.any()
        );
    }
}
