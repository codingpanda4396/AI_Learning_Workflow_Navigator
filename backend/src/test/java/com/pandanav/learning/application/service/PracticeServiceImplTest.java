package com.pandanav.learning.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pandanav.learning.application.service.practice.LlmPracticeGenerator;
import com.pandanav.learning.application.service.practice.PracticeDraftItem;
import com.pandanav.learning.application.service.practice.PracticeGeneratorResult;
import com.pandanav.learning.application.service.practice.RulePracticeGenerator;
import com.pandanav.learning.domain.enums.PracticeQuestionType;
import com.pandanav.learning.domain.enums.Stage;
import com.pandanav.learning.domain.enums.TaskStatus;
import com.pandanav.learning.domain.model.ConceptNode;
import com.pandanav.learning.domain.model.LearningSession;
import com.pandanav.learning.domain.model.PracticeItem;
import com.pandanav.learning.domain.model.Task;
import com.pandanav.learning.domain.repository.ConceptNodeRepository;
import com.pandanav.learning.domain.repository.LearningEventRepository;
import com.pandanav.learning.domain.repository.PracticeRepository;
import com.pandanav.learning.domain.repository.PracticeSubmissionRepository;
import com.pandanav.learning.domain.repository.SessionRepository;
import com.pandanav.learning.domain.repository.TaskRepository;
import com.pandanav.learning.infrastructure.config.LlmProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

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
    private TaskRepository taskRepository;
    @Mock
    private SessionRepository sessionRepository;
    @Mock
    private ConceptNodeRepository conceptNodeRepository;
    @Mock
    private LearningEventRepository learningEventRepository;
    @Mock
    private RulePracticeGenerator rulePracticeGenerator;
    @Mock
    private LlmPracticeGenerator llmPracticeGenerator;

    private LlmProperties llmProperties;
    private PracticeServiceImpl practiceService;

    @BeforeEach
    void setUp() {
        llmProperties = new LlmProperties();
        practiceService = new PracticeServiceImpl(
            practiceRepository,
            practiceSubmissionRepository,
            taskRepository,
            sessionRepository,
            conceptNodeRepository,
            learningEventRepository,
            rulePracticeGenerator,
            llmPracticeGenerator,
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
        verify(rulePracticeGenerator, never()).generate(any());
    }

    @Test
    void shouldFallbackToRuleWhenLlmFails() {
        Long sessionId = 201L;
        Long taskId = 101L;
        Long userId = 11L;

        llmProperties.setEnabled(true);
        llmProperties.setBaseUrl("http://localhost");
        llmProperties.setApiKey("test-key");
        llmProperties.setModel("test-model");
        llmProperties.setFallbackToRule(true);

        when(taskRepository.findByIdAndUserPk(taskId, userId)).thenReturn(Optional.of(trainingTask(taskId, sessionId)));
        when(sessionRepository.findByIdAndUserPk(sessionId, userId)).thenReturn(Optional.of(new LearningSession()));
        when(practiceRepository.findBySessionIdAndTaskIdAndUserPk(sessionId, taskId, userId)).thenReturn(List.of());

        ConceptNode node = new ConceptNode();
        node.setId(301L);
        node.setName("TCP Handshake");
        when(conceptNodeRepository.findById(301L)).thenReturn(Optional.of(node));

        when(llmPracticeGenerator.generate(any())).thenThrow(new RuntimeException("llm broken"));
        when(rulePracticeGenerator.generate(any())).thenReturn(new PracticeGeneratorResult(
            List.of(
                draft(PracticeQuestionType.SINGLE_CHOICE),
                draft(PracticeQuestionType.TRUE_FALSE),
                draft(PracticeQuestionType.SHORT_ANSWER)
            ),
            "RULE",
            false,
            false,
            "rule-v1",
            null,
            null,
            null,
            null,
            null
        ));

        AtomicLong idGen = new AtomicLong(1000L);
        when(practiceRepository.save(any(PracticeItem.class))).thenAnswer(invocation -> {
            PracticeItem item = invocation.getArgument(0);
            item.setId(idGen.incrementAndGet());
            return item;
        });

        List<PracticeItem> items = practiceService.getOrCreatePracticeItems(sessionId, taskId, userId);

        assertEquals(3, items.size());
        assertEquals("RULE", items.get(0).getSource().name());
        verify(llmPracticeGenerator).generate(any());
        verify(rulePracticeGenerator).generate(any());
        verify(learningEventRepository).save(any());
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

    private PracticeDraftItem draft(PracticeQuestionType questionType) {
        return new PracticeDraftItem(
            questionType,
            "stem",
            List.of("A", "B"),
            "A",
            "explanation",
            "EASY"
        );
    }
}
