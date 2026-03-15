package com.pandanav.learning.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pandanav.learning.api.dto.task.SubmitTaskRequest;
import com.pandanav.learning.api.dto.task.SubmitTaskResponse;
import com.pandanav.learning.application.service.MasteryUpdateService.MasteryUpdateResult;
import com.pandanav.learning.domain.llm.AnswerEvaluator;
import com.pandanav.learning.domain.llm.model.EvaluationResult;
import com.pandanav.learning.domain.enums.NextAction;
import com.pandanav.learning.domain.enums.Stage;
import com.pandanav.learning.domain.enums.TaskStatus;
import com.pandanav.learning.domain.model.ConceptNode;
import com.pandanav.learning.domain.model.LearningSession;
import com.pandanav.learning.domain.model.Task;
import com.pandanav.learning.domain.policy.NextActionDecision;
import com.pandanav.learning.domain.repository.ConceptNodeRepository;
import com.pandanav.learning.domain.repository.EvidenceRepository;
import com.pandanav.learning.domain.repository.LearningStepRepository;
import com.pandanav.learning.domain.repository.LlmCallLogRepository;
import com.pandanav.learning.domain.repository.SessionRepository;
import com.pandanav.learning.domain.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubmitTrainingAnswerServiceTest {

    @Mock
    private TaskRepository taskRepository;
    @Mock
    private SessionRepository sessionRepository;
    @Mock
    private ConceptNodeRepository conceptNodeRepository;
    @Mock
    private EvidenceRepository evidenceRepository;
    @Mock
    private LearningStepRepository learningStepRepository;
    @Mock
    private LlmCallLogRepository llmCallLogRepository;
    @Mock
    private AnswerEvaluator answerEvaluator;
    @Mock
    private MasteryUpdateService masteryUpdateService;
    @Mock
    private NextActionPolicyService nextActionPolicyService;

    private SubmitTrainingAnswerService submitTrainingAnswerService;

    @BeforeEach
    void setUp() {
        submitTrainingAnswerService = new SubmitTrainingAnswerService(
            taskRepository,
            sessionRepository,
            conceptNodeRepository,
            evidenceRepository,
            learningStepRepository,
            llmCallLogRepository,
            answerEvaluator,
            masteryUpdateService,
            nextActionPolicyService,
            new ObjectMapper()
        );
    }

    @Test
    void shouldInsertTrainingVariantTaskWhenPolicyRequiresIt() {
        Task currentTask = trainingTask(1003L, 123L, 101L, TaskStatus.SUCCEEDED);
        LearningSession session = session(123L, "u1", "tcp", 101L);
        ConceptNode node = node(101L, "Three-way Handshake", "tcp", 1);

        when(taskRepository.findById(1003L)).thenReturn(Optional.of(currentTask));
        when(sessionRepository.findById(123L)).thenReturn(Optional.of(session));
        when(conceptNodeRepository.findById(101L)).thenReturn(Optional.of(node));
        when(answerEvaluator.evaluate(any())).thenReturn(
            new EvaluationResult(
                72,
                new BigDecimal("0.720"),
                "diag",
                List.of("MISSING_STEPS"),
                List.of("s1", "s2"),
                List.of("fix1", "fix2"),
                "INSERT_TRAINING_VARIANTS",
                new ObjectMapper().createObjectNode(),
                null,
                null,
                null,
                "EVALUATE",
                "rule-v1",
                null,
                null,
                null
            )
        );
        when(masteryUpdateService.update("u1", 101L, "Three-way Handshake", 72)).thenReturn(
            new MasteryUpdateResult(new BigDecimal("0.550"), new BigDecimal("0.010"), new BigDecimal("0.560"))
        );
        when(nextActionPolicyService.decideWithReason(any(), any(), any(), anyInt(), anyInt()))
            .thenReturn(new NextActionDecision(NextAction.INSERT_TRAINING_VARIANTS, "need variants"));
        when(learningStepRepository.findByTaskIdOrderByStepOrder(any())).thenReturn(List.of());
        when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> {
            Task t = invocation.getArgument(0);
            t.setId(2001L);
            return t;
        });

        SubmitTaskResponse response = submitTrainingAnswerService.submit(1003L, new SubmitTaskRequest("answer"));

        assertEquals(1003L, response.taskId());
        assertEquals("TRAINING", response.stage());
        assertEquals(101L, response.nodeId());
        assertEquals(72, response.score());
        assertEquals(new BigDecimal("0.720"), response.normalizedScore());
        assertEquals("INSERT_TRAINING_VARIANTS", response.nextAction());
        assertNotNull(response.nextTask());
        assertEquals(2001L, response.nextTask().taskId());
        assertEquals("TRAINING", response.nextTask().stage());

        verify(evidenceRepository).save(any());
        ArgumentCaptor<Task> createdTaskCaptor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(createdTaskCaptor.capture());
        assertEquals(Stage.TRAINING, createdTaskCaptor.getValue().getStage());
    }

    private Task trainingTask(Long taskId, Long sessionId, Long nodeId, TaskStatus status) {
        Task task = new Task();
        task.setId(taskId);
        task.setSessionId(sessionId);
        task.setNodeId(nodeId);
        task.setStage(Stage.TRAINING);
        task.setStatus(status);
        task.setObjective("Explain three-way handshake");
        return task;
    }

    private LearningSession session(Long sessionId, String userId, String chapterId, Long currentNodeId) {
        LearningSession session = new LearningSession();
        session.setId(sessionId);
        session.setUserId(userId);
        session.setChapterId(chapterId);
        session.setCurrentNodeId(currentNodeId);
        return session;
    }

    private ConceptNode node(Long nodeId, String name, String chapterId, int orderNo) {
        ConceptNode node = new ConceptNode();
        node.setId(nodeId);
        node.setName(name);
        node.setChapterId(chapterId);
        node.setOrderNo(orderNo);
        return node;
    }
}
