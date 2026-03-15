package com.pandanav.learning.application.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pandanav.learning.api.dto.feedback.LearningReportResponse;
import com.pandanav.learning.api.dto.session.GrowthDashboardResponse;
import com.pandanav.learning.domain.enums.Stage;
import com.pandanav.learning.domain.model.ConceptNode;
import com.pandanav.learning.domain.model.LearningSession;
import com.pandanav.learning.domain.model.NodeMastery;
import com.pandanav.learning.domain.model.PracticeFeedbackReport;
import com.pandanav.learning.domain.model.PracticeItem;
import com.pandanav.learning.domain.model.PracticeQuiz;
import com.pandanav.learning.domain.model.PracticeSubmission;
import com.pandanav.learning.domain.model.Task;
import com.pandanav.learning.domain.model.TrainingAttemptSummary;
import com.pandanav.learning.domain.repository.ConceptNodeRepository;
import com.pandanav.learning.domain.repository.EvidenceRepository;
import com.pandanav.learning.domain.repository.NodeMasteryRepository;
import com.pandanav.learning.domain.repository.PracticeFeedbackReportRepository;
import com.pandanav.learning.domain.repository.PracticeQuizRepository;
import com.pandanav.learning.domain.repository.PracticeRepository;
import com.pandanav.learning.domain.repository.PracticeSubmissionRepository;
import com.pandanav.learning.domain.repository.SessionRepository;
import com.pandanav.learning.domain.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LearningInsightQueryServiceTest {

    @Mock
    private SessionRepository sessionRepository;
    @Mock
    private TaskRepository taskRepository;
    @Mock
    private PracticeQuizRepository practiceQuizRepository;
    @Mock
    private PracticeFeedbackReportRepository practiceFeedbackReportRepository;
    @Mock
    private PracticeRepository practiceRepository;
    @Mock
    private PracticeSubmissionRepository practiceSubmissionRepository;
    @Mock
    private NodeMasteryRepository nodeMasteryRepository;
    @Mock
    private ConceptNodeRepository conceptNodeRepository;
    @Mock
    private EvidenceRepository evidenceRepository;
    @Mock
    private WeakPointDiagnosisService weakPointDiagnosisService;

    private LearningInsightQueryService learningInsightQueryService;

    @BeforeEach
    void setUp() {
        learningInsightQueryService = new LearningInsightQueryService(
            sessionRepository,
            taskRepository,
            practiceQuizRepository,
            practiceFeedbackReportRepository,
            practiceRepository,
            practiceSubmissionRepository,
            nodeMasteryRepository,
            conceptNodeRepository,
            evidenceRepository,
            weakPointDiagnosisService,
            new ObjectMapper()
        );
    }

    @Test
    void shouldBuildLearningReportWithRecommendation() {
        LearningSession session = buildSession();
        Task task = buildTrainingTask();
        ConceptNode currentNode = buildNode(101L, "Binary Search", 1);
        PracticeQuiz quiz = new PracticeQuiz();
        quiz.setId(900L);
        PracticeFeedbackReport report = new PracticeFeedbackReport();
        report.setDiagnosisSummary("需要继续强化边界条件。");
        report.setStrengthsJson("[\"已完成整轮训练\"]");
        report.setWeaknessesJson("[\"边界条件仍有遗漏\"]");
        report.setReviewFocusJson("[\"BOUNDARY_CASE\"]");

        PracticeItem item = new PracticeItem();
        item.setId(501L);
        item.setStem("请说明二分查找循环终止条件");
        PracticeSubmission submission = new PracticeSubmission();
        submission.setPracticeItemId(501L);
        submission.setScore(58);
        submission.setCorrect(false);
        submission.setFeedback("边界条件处理不完整");
        submission.setErrorTagsJson("[\"BOUNDARY_CASE\"]");

        NodeMastery mastery = new NodeMastery();
        mastery.setNodeId(101L);
        mastery.setNodeName("Binary Search");
        mastery.setMasteryScore(new BigDecimal("62.00"));
        mastery.setTrainingAccuracy(new BigDecimal("58.00"));
        mastery.setAttemptCount(2);

        WeakPointDiagnosisService.WeakNode weakNode = new WeakPointDiagnosisService.WeakNode(
            101L,
            "Binary Search",
            new BigDecimal("62.00"),
            new BigDecimal("58.00"),
            58,
            2,
            List.of("BOUNDARY_CASE"),
            List.of("LOW_TRAINING_ACCURACY")
        );

        when(sessionRepository.findByIdAndUserPk(200L, 10L)).thenReturn(Optional.of(session));
        when(taskRepository.findBySessionIdWithStatus(200L)).thenReturn(List.of(task));
        when(conceptNodeRepository.findById(101L)).thenReturn(Optional.of(currentNode));
        when(conceptNodeRepository.findByChapterIdOrderByOrderNoAsc("ch1")).thenReturn(List.of(currentNode));
        when(practiceQuizRepository.findLatestBySessionIdAndTaskIdAndUserPk(200L, 300L, 10L)).thenReturn(Optional.of(quiz));
        when(practiceFeedbackReportRepository.findByQuizId(900L)).thenReturn(Optional.of(report));
        when(practiceRepository.findByQuizId(900L)).thenReturn(List.of(item));
        when(practiceSubmissionRepository.findBySessionIdAndTaskIdAndUserPk(200L, 300L, 10L)).thenReturn(List.of(submission));
        when(nodeMasteryRepository.findByUserIdAndNodeId(10L, 101L)).thenReturn(Optional.of(mastery));
        when(weakPointDiagnosisService.diagnoseWeakPoints(200L, 10L))
            .thenReturn(new WeakPointDiagnosisService.WeakPointDiagnosisResult(200L, 10L, "summary", List.of(weakNode)));

        LearningReportResponse response = learningInsightQueryService.getLearningReport(200L, 10L);

        assertEquals("REINFORCE", response.nextStep().recommendedAction());
        assertEquals(1, response.questionResults().size());
        assertEquals("训练应用", response.stageLabel());
        assertTrue(response.growthRecorded());
    }

    @Test
    void shouldBuildGrowthDashboard() {
        LearningSession session = buildSession();
        Task task = buildTrainingTask();
        ConceptNode currentNode = buildNode(101L, "Binary Search", 1);
        ConceptNode nextNode = buildNode(102L, "Divide and Conquer", 2);

        NodeMastery mastery = new NodeMastery();
        mastery.setNodeId(101L);
        mastery.setNodeName("Binary Search");
        mastery.setMasteryScore(new BigDecimal("88.00"));
        mastery.setTrainingAccuracy(new BigDecimal("90.00"));
        mastery.setAttemptCount(4);

        PracticeSubmission submission = new PracticeSubmission();
        submission.setPracticeItemId(501L);
        submission.setScore(92);
        submission.setCorrect(true);
        submission.setErrorTagsJson("[]");

        WeakPointDiagnosisService.WeakNode weakNode = new WeakPointDiagnosisService.WeakNode(
            102L,
            "Divide and Conquer",
            new BigDecimal("55.00"),
            new BigDecimal("60.00"),
            60,
            2,
            List.of("CONCEPT_CONFUSION"),
            List.of("LOW_MASTERY_SCORE")
        );

        when(sessionRepository.findByIdAndUserPk(200L, 10L)).thenReturn(Optional.of(session));
        when(taskRepository.findBySessionIdWithStatus(200L)).thenReturn(List.of(task));
        when(taskRepository.findRecentTrainingAttempts(200L, 5))
            .thenReturn(List.of(new TrainingAttemptSummary(300L, 101L, 92, List.of("BOUNDARY_CASE"))));
        when(conceptNodeRepository.findById(101L)).thenReturn(Optional.of(currentNode));
        when(conceptNodeRepository.findByChapterIdOrderByOrderNoAsc("ch1")).thenReturn(List.of(currentNode, nextNode));
        when(nodeMasteryRepository.findByUserIdAndChapterId(10L, "ch1")).thenReturn(List.of(mastery));
        when(practiceSubmissionRepository.findBySessionIdAndTaskIdAndUserPk(200L, 300L, 10L)).thenReturn(List.of(submission));
        when(weakPointDiagnosisService.diagnoseWeakPoints(200L, 10L))
            .thenReturn(new WeakPointDiagnosisService.WeakPointDiagnosisResult(200L, 10L, "summary", List.of(weakNode)));

        GrowthDashboardResponse response = learningInsightQueryService.getGrowthDashboard(200L, 10L);

        assertEquals(1, response.learnedNodeCount());
        assertEquals("ADVANCE", response.recommendedNextStep().recommendedAction());
        assertEquals(2, response.masteryNodes().size());
    }

    private LearningSession buildSession() {
        LearningSession session = new LearningSession();
        session.setId(200L);
        session.setUserPk(10L);
        session.setCourseId("cs101");
        session.setChapterId("ch1");
        session.setCurrentNodeId(101L);
        session.setCurrentStage(Stage.TRAINING);
        return session;
    }

    private Task buildTrainingTask() {
        Task task = new Task();
        task.setId(300L);
        task.setSessionId(200L);
        task.setNodeId(101L);
        task.setStage(Stage.TRAINING);
        task.setCreatedAt(OffsetDateTime.now());
        return task;
    }

    private ConceptNode buildNode(Long id, String name, int orderNo) {
        ConceptNode node = new ConceptNode();
        node.setId(id);
        node.setName(name);
        node.setOrderNo(orderNo);
        node.setChapterId("ch1");
        return node;
    }
}
