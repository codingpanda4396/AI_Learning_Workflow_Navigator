package com.pandanav.learning.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pandanav.learning.domain.model.LearningSession;
import com.pandanav.learning.domain.model.NodeMastery;
import com.pandanav.learning.domain.repository.NodeMasteryRepository;
import com.pandanav.learning.domain.repository.SessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WeakPointDiagnosisServiceTest {

    @Mock
    private SessionRepository sessionRepository;
    @Mock
    private NodeMasteryRepository nodeMasteryRepository;

    private WeakPointDiagnosisService weakPointDiagnosisService;

    @BeforeEach
    void setUp() {
        weakPointDiagnosisService = new WeakPointDiagnosisService(
            sessionRepository,
            nodeMasteryRepository,
            new ObjectMapper()
        );
    }

    @Test
    void shouldDiagnoseWeakNodesByMasteryAccuracyAndTags() {
        LearningSession session = new LearningSession();
        session.setId(200L);
        session.setChapterId("ch1");

        NodeMastery weak = new NodeMastery();
        weak.setNodeId(301L);
        weak.setNodeName("Binary Search");
        weak.setMasteryScore(new BigDecimal("52.00"));
        weak.setTrainingAccuracy(new BigDecimal("60.00"));
        weak.setAttemptCount(3);
        weak.setRecentErrorTagsJson("[\"MISSING_STEPS\",\"CONCEPT_CONFUSION\"]");

        NodeMastery strong = new NodeMastery();
        strong.setNodeId(302L);
        strong.setNodeName("Sorting");
        strong.setMasteryScore(new BigDecimal("88.00"));
        strong.setTrainingAccuracy(new BigDecimal("92.00"));
        strong.setAttemptCount(4);
        strong.setRecentErrorTagsJson("[]");

        when(sessionRepository.findByIdAndUserPk(200L, 10L)).thenReturn(Optional.of(session));
        when(nodeMasteryRepository.findByUserIdAndChapterId(10L, "ch1")).thenReturn(List.of(weak, strong));

        WeakPointDiagnosisService.WeakPointDiagnosisResult result = weakPointDiagnosisService.diagnoseWeakPoints(200L, 10L);

        assertEquals(1, result.weakNodes().size());
        assertEquals(301L, result.weakNodes().get(0).nodeId());
        org.junit.jupiter.api.Assertions.assertTrue(result.diagnosisSummary().contains("weak nodes"));
    }
}
