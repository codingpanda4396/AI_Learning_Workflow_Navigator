package com.pandanav.learning.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pandanav.learning.application.service.practice.LlmPracticeGenerator;
import com.pandanav.learning.domain.enums.PracticeQuestionType;
import com.pandanav.learning.domain.enums.Stage;
import com.pandanav.learning.domain.enums.TaskStatus;
import com.pandanav.learning.domain.model.ConceptNode;
import com.pandanav.learning.domain.model.LearningSession;
import com.pandanav.learning.domain.model.PracticeItem;
import com.pandanav.learning.domain.model.Task;
import com.pandanav.learning.domain.repository.ConceptNodeRepository;
import com.pandanav.learning.domain.repository.LearningEventRepository;
import com.pandanav.learning.domain.repository.PracticeFeedbackReportRepository;
import com.pandanav.learning.domain.repository.PracticeQuizRepository;
import com.pandanav.learning.domain.repository.PracticeRepository;
import com.pandanav.learning.domain.repository.PracticeSubmissionRepository;
import com.pandanav.learning.domain.repository.SessionRepository;
import com.pandanav.learning.domain.repository.TaskRepository;
import com.pandanav.learning.infrastructure.config.LlmProperties;
import com.pandanav.learning.infrastructure.exception.AiGenerationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PracticeServiceImplTest {

    @Mock
    private PracticeRepository practiceRepository;
    @Mock
    private PracticeSubmissionRepository practiceSubmissionRepository;
    @Mock
    private PracticeQuizRepository practiceQuizRepository;
    @Mock
    private PracticeFeedbackReportRepository practiceFeedbackReportRepository;
    @Mock
    private TaskRepository taskRepository;
    @Mock
    private SessionRepository sessionRepository;
    @Mock
    private ConceptNodeRepository conceptNodeRepository;
    @Mock
    private LearningEventRepository learningEventRepository;
    @Mock
    private LlmPracticeGenerator llmPracticeGenerator;
    @Mock
    private PracticeFeedbackReportGenerator practiceFeedbackReportGenerator;
    @Mock
    private PracticeQuizAsyncService practiceQuizAsyncService;
    @Mock
    private MasteryService masteryService;

    private LlmProperties llmProperties;
    private PracticeServiceImpl practiceService;

    @BeforeEach
    void setUp() {
        llmProperties = new LlmProperties();
        practiceService = new PracticeServiceImpl(
            practiceRepository,
            practiceSubmissionRepository,
            practiceQuizRepository,
            practiceFeedbackReportRepository,
            taskRepository,
            sessionRepository,
            conceptNodeRepository,
            learningEventRepository,
            llmPracticeGenerator,
            practiceFeedbackReportGenerator,
            practiceQuizAsyncService,
            masteryService,
            llmProperties,
            new ObjectMapper()
        );
    }

    @Test
    void shouldReuseExistingItemsWhenAlreadyGenerated() {
        Long sessionId = 200L;
        Long taskId = 100L;
        Long userId = 10L;

        when(taskRepository.findByIdAndUserPk(taskId, userId)).thenReturn(Optional.of(trainingTask(taskId, sessionId)));
        when(sessionRepository.findByIdAndUserPk(sessionId, userId)).thenReturn(Optional.of(new LearningSession()));
        when(practiceRepository.findBySessionIdAndTaskIdAndUserPk(sessionId, taskId, userId)).thenReturn(List.of(practiceItem(1L)));

        List<PracticeItem> result = practiceService.getOrCreatePracticeItems(sessionId, taskId, userId);

        assertEquals(1, result.size());
        verify(llmPracticeGenerator, never()).generate(any());
    }

    @Test
    void shouldFailWhenLlmFails() {
        Long sessionId = 201L;
        Long taskId = 101L;
        Long userId = 11L;

        llmProperties.setEnabled(true);
        llmProperties.setBaseUrl("http://localhost");
        llmProperties.setApiKey("test-key");
        llmProperties.setModel("test-model");

        when(taskRepository.findByIdAndUserPk(taskId, userId)).thenReturn(Optional.of(trainingTask(taskId, sessionId)));
        when(sessionRepository.findByIdAndUserPk(sessionId, userId)).thenReturn(Optional.of(new LearningSession()));
        when(practiceRepository.findBySessionIdAndTaskIdAndUserPk(sessionId, taskId, userId)).thenReturn(List.of());

        ConceptNode node = new ConceptNode();
        node.setId(301L);
        node.setName("TCP Handshake");
        when(conceptNodeRepository.findById(301L)).thenReturn(Optional.of(node));

        when(llmPracticeGenerator.generate(any())).thenThrow(new AiGenerationException("PRACTICE_GENERATION", "UNKNOWN_ERROR"));

        org.junit.jupiter.api.Assertions.assertThrows(
            AiGenerationException.class,
            () -> practiceService.getOrCreatePracticeItems(sessionId, taskId, userId)
        );
        verify(llmPracticeGenerator).generate(any());
    }

    private Task trainingTask(Long taskId, Long sessionId) {
        Task task = new Task();
        task.setId(taskId);
        task.setSessionId(sessionId);
        task.setNodeId(301L);
        task.setStage(Stage.TRAINING);
        task.setStatus(TaskStatus.SUCCEEDED);
        task.setObjective("Explain TCP handshake");
        task.setOutputJson("{\"summary\":\"ok\"}");
        return task;
    }

    private PracticeItem practiceItem(Long id) {
        PracticeItem item = new PracticeItem();
        item.setId(id);
        item.setQuestionType(PracticeQuestionType.SINGLE_CHOICE);
        item.setStem("existing");
        return item;
    }
}
