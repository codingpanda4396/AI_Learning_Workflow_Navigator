package com.pandanav.learning.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pandanav.learning.application.service.practice.LlmPracticeGenerator;
import com.pandanav.learning.domain.enums.PracticeItemSource;
import com.pandanav.learning.domain.enums.PracticeItemStatus;
import com.pandanav.learning.domain.enums.PracticeQuestionType;
import com.pandanav.learning.domain.enums.PracticeQuizStatus;
import com.pandanav.learning.domain.enums.Stage;
import com.pandanav.learning.domain.model.LearningSession;
import com.pandanav.learning.domain.model.PracticeItem;
import com.pandanav.learning.domain.model.PracticeQuiz;
import com.pandanav.learning.domain.model.PracticeSubmission;
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
import com.pandanav.learning.infrastructure.exception.ConflictException;
import com.pandanav.learning.infrastructure.exception.NotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PracticeSubmissionServiceTest {

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

    private PracticeServiceImpl practiceService;

    @BeforeEach
    void setUp() {
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
            new LlmProperties(),
            new ObjectMapper()
        );
    }

    @Test
    void shouldRejectSubmitWhenTaskNotOwnedByUser() {
        when(taskRepository.findByIdAndUserPk(100L, 10L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () ->
            practiceService.submitPracticeAnswer(200L, 100L, 300L, 10L, "A")
        );
    }

    @Test
    void shouldRejectInvalidPracticeItemId() {
        when(taskRepository.findByIdAndUserPk(100L, 10L)).thenReturn(Optional.of(trainingTask()));
        when(sessionRepository.findByIdAndUserPk(200L, 10L)).thenReturn(Optional.of(new LearningSession()));
        when(practiceRepository.findByIdAndUserPk(300L, 10L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () ->
            practiceService.submitPracticeAnswer(200L, 100L, 300L, 10L, "A")
        );
    }

    @Test
    void shouldAllowRepeatedSubmissionAndMarkRepeatedEvent() {
        PracticeItem item = singleChoiceItem();
        when(taskRepository.findByIdAndUserPk(100L, 10L)).thenReturn(Optional.of(trainingTask()));
        when(sessionRepository.findByIdAndUserPk(200L, 10L)).thenReturn(Optional.of(new LearningSession()));
        when(practiceRepository.findByIdAndUserPk(300L, 10L)).thenReturn(Optional.of(item));
        PracticeQuiz quiz = new PracticeQuiz();
        quiz.setId(400L);
        quiz.setSessionId(200L);
        quiz.setTaskId(100L);
        quiz.setStatus(PracticeQuizStatus.ANSWERING);
        when(practiceQuizRepository.findLatestBySessionIdAndTaskIdAndUserPk(200L, 100L, 10L)).thenReturn(Optional.of(quiz));
        when(practiceRepository.findByQuizId(400L)).thenReturn(java.util.List.of(item));
        when(practiceSubmissionRepository.findByQuizId(400L)).thenReturn(java.util.List.of());

        AtomicLong id = new AtomicLong(500L);
        when(practiceSubmissionRepository.save(any(PracticeSubmission.class))).thenAnswer(invocation -> {
            PracticeSubmission submission = invocation.getArgument(0);
            submission.setId(id.incrementAndGet());
            return submission;
        });

        when(practiceSubmissionRepository.findLatestByPracticeItemIdAndUserPk(300L, 10L))
            .thenReturn(Optional.empty())
            .thenReturn(Optional.of(new PracticeSubmission()));

        practiceService.submitPracticeAnswer(200L, 100L, 300L, 10L, "A");
        practiceService.submitPracticeAnswer(200L, 100L, 300L, 10L, "B");

        verify(practiceSubmissionRepository, times(2)).save(any(PracticeSubmission.class));
        verify(practiceRepository, times(2)).updateStatus(300L, PracticeItemStatus.ANSWERED);

        ArgumentCaptor<com.pandanav.learning.domain.model.LearningEvent> eventCaptor = ArgumentCaptor.forClass(
            com.pandanav.learning.domain.model.LearningEvent.class
        );
        verify(learningEventRepository, times(2)).save(eventCaptor.capture());
        String latestPayload = eventCaptor.getAllValues().get(1).getEventData();
        org.junit.jupiter.api.Assertions.assertTrue(latestPayload.contains("\"repeated_submission\":true"));
    }

    @Test
    void shouldRejectSubmitWhenTaskIsNotTrainingStage() {
        Task nonTraining = trainingTask();
        nonTraining.setStage(Stage.UNDERSTANDING);
        when(taskRepository.findByIdAndUserPk(100L, 10L)).thenReturn(Optional.of(nonTraining));

        assertThrows(ConflictException.class, () ->
            practiceService.submitPracticeAnswer(200L, 100L, 300L, 10L, "A")
        );
    }

    private Task trainingTask() {
        Task task = new Task();
        task.setId(100L);
        task.setSessionId(200L);
        task.setNodeId(900L);
        task.setStage(Stage.TRAINING);
        return task;
    }

    private PracticeItem singleChoiceItem() {
        PracticeItem item = new PracticeItem();
        item.setId(300L);
        item.setSessionId(200L);
        item.setTaskId(100L);
        item.setUserId(10L);
        item.setStage(Stage.TRAINING);
        item.setQuestionType(PracticeQuestionType.SINGLE_CHOICE);
        item.setStandardAnswer("A");
        item.setSource(PracticeItemSource.RULE);
        item.setStatus(PracticeItemStatus.READY);
        return item;
    }
}
