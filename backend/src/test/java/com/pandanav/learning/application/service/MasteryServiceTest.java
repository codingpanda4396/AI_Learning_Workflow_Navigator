package com.pandanav.learning.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pandanav.learning.domain.enums.Stage;
import com.pandanav.learning.domain.model.LearningSession;
import com.pandanav.learning.domain.model.NodeMastery;
import com.pandanav.learning.domain.model.PracticeSubmission;
import com.pandanav.learning.domain.model.Task;
import com.pandanav.learning.domain.repository.LearningEventRepository;
import com.pandanav.learning.domain.repository.MasteryRepository;
import com.pandanav.learning.domain.repository.NodeMasteryRepository;
import com.pandanav.learning.domain.repository.PracticeSubmissionRepository;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MasteryServiceTest {

    @Mock
    private SessionRepository sessionRepository;
    @Mock
    private TaskRepository taskRepository;
    @Mock
    private PracticeSubmissionRepository practiceSubmissionRepository;
    @Mock
    private NodeMasteryRepository nodeMasteryRepository;
    @Mock
    private MasteryRepository masteryRepository;
    @Mock
    private LearningEventRepository learningEventRepository;

    private MasteryService masteryService;

    @BeforeEach
    void setUp() {
        masteryService = new MasteryService(
            sessionRepository,
            taskRepository,
            practiceSubmissionRepository,
            nodeMasteryRepository,
            masteryRepository,
            learningEventRepository,
            new ObjectMapper()
        );
    }

    @Test
    void shouldRecalculateNodeMasteryFromPracticeSubmissions() {
        LearningSession session = new LearningSession();
        session.setId(200L);
        session.setUserPk(10L);
        session.setUserId("u1");

        Task task = new Task();
        task.setId(100L);
        task.setSessionId(200L);
        task.setNodeId(301L);
        task.setStage(Stage.TRAINING);

        PracticeSubmission s1 = new PracticeSubmission();
        s1.setCorrect(false);
        s1.setErrorTagsJson("[\"MISSING_STEPS\",\"CONCEPT_CONFUSION\"]");
        PracticeSubmission s2 = new PracticeSubmission();
        s2.setCorrect(false);
        s2.setErrorTagsJson("[\"MISSING_STEPS\"]");
        PracticeSubmission s3 = new PracticeSubmission();
        s3.setCorrect(true);
        s3.setErrorTagsJson("[]");

        when(sessionRepository.findByIdAndUserPk(200L, 10L)).thenReturn(Optional.of(session));
        when(taskRepository.findByIdAndUserPk(100L, 10L)).thenReturn(Optional.of(task));
        when(practiceSubmissionRepository.findBySessionIdAndTaskIdAndUserPk(200L, 100L, 10L))
            .thenReturn(List.of(s1, s2, s3));
        when(taskRepository.findLatestScoreByTaskId(100L)).thenReturn(Optional.of(75));
        when(nodeMasteryRepository.upsert(any(NodeMastery.class))).thenAnswer(inv -> inv.getArgument(0));

        NodeMastery result = masteryService.recalculateNodeMastery(200L, 100L, 10L);

        assertEquals(3, result.getAttemptCount());
        assertEquals(new BigDecimal("33.33"), result.getTrainingAccuracy());
        verify(nodeMasteryRepository).upsert(any(NodeMastery.class));
        verify(masteryRepository).upsert(any());
        verify(learningEventRepository).save(any());

        ArgumentCaptor<NodeMastery> captor = ArgumentCaptor.forClass(NodeMastery.class);
        verify(nodeMasteryRepository).upsert(captor.capture());
        org.junit.jupiter.api.Assertions.assertTrue(captor.getValue().getRecentErrorTagsJson().contains("MISSING_STEPS"));
    }
}
