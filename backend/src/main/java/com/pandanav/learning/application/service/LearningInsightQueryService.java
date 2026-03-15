package com.pandanav.learning.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pandanav.learning.api.dto.feedback.LearningReportMasteryResponse;
import com.pandanav.learning.api.dto.feedback.LearningReportQuestionResponse;
import com.pandanav.learning.api.dto.feedback.LearningReportResponse;
import com.pandanav.learning.api.dto.feedback.NextStepRecommendationResponse;
import com.pandanav.learning.api.dto.feedback.SessionReportResponse;
import com.pandanav.learning.api.dto.feedback.StepEvidenceResponse;
import com.pandanav.learning.api.dto.feedback.WeakPointNodeResponse;
import com.pandanav.learning.api.dto.session.GrowthDashboardNodeResponse;
import com.pandanav.learning.api.dto.session.GrowthDashboardRecentPerformanceResponse;
import com.pandanav.learning.api.dto.session.GrowthDashboardResponse;
import com.pandanav.learning.domain.enums.Stage;
import com.pandanav.learning.domain.model.ConceptNode;
import com.pandanav.learning.domain.model.Evidence;
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
import com.pandanav.learning.infrastructure.exception.NotFoundException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class LearningInsightQueryService {

    private static final String NOT_FOUND_MESSAGE = "Session or task not found.";

    private final SessionRepository sessionRepository;
    private final TaskRepository taskRepository;
    private final PracticeQuizRepository practiceQuizRepository;
    private final PracticeFeedbackReportRepository practiceFeedbackReportRepository;
    private final PracticeRepository practiceRepository;
    private final PracticeSubmissionRepository practiceSubmissionRepository;
    private final NodeMasteryRepository nodeMasteryRepository;
    private final ConceptNodeRepository conceptNodeRepository;
    private final EvidenceRepository evidenceRepository;
    private final WeakPointDiagnosisService weakPointDiagnosisService;
    private final ObjectMapper objectMapper;

    public LearningInsightQueryService(
        SessionRepository sessionRepository,
        TaskRepository taskRepository,
        PracticeQuizRepository practiceQuizRepository,
        PracticeFeedbackReportRepository practiceFeedbackReportRepository,
        PracticeRepository practiceRepository,
        PracticeSubmissionRepository practiceSubmissionRepository,
        NodeMasteryRepository nodeMasteryRepository,
        ConceptNodeRepository conceptNodeRepository,
        EvidenceRepository evidenceRepository,
        WeakPointDiagnosisService weakPointDiagnosisService,
        ObjectMapper objectMapper
    ) {
        this.sessionRepository = sessionRepository;
        this.taskRepository = taskRepository;
        this.practiceQuizRepository = practiceQuizRepository;
        this.practiceFeedbackReportRepository = practiceFeedbackReportRepository;
        this.practiceRepository = practiceRepository;
        this.practiceSubmissionRepository = practiceSubmissionRepository;
        this.nodeMasteryRepository = nodeMasteryRepository;
        this.conceptNodeRepository = conceptNodeRepository;
        this.evidenceRepository = evidenceRepository;
        this.weakPointDiagnosisService = weakPointDiagnosisService;
        this.objectMapper = objectMapper;
    }

    public LearningReportResponse getLearningReport(Long sessionId, Long userId) {
        LearningReportView reportView = buildLearningReportView(sessionId, userId);
        return new LearningReportResponse(
            reportView.sessionId(),
            reportView.taskId(),
            reportView.nodeId(),
            reportView.nodeName(),
            reportView.stageCode(),
            reportView.stageLabel(),
            reportView.overallScore(),
            reportView.overallAccuracy(),
            reportView.correctCount(),
            reportView.questionCount(),
            reportView.diagnosisSummary(),
            reportView.strengths(),
            reportView.weaknesses(),
            reportView.reviewFocus(),
            reportView.mastery(),
            reportView.questionResults(),
            reportView.weakPoints(),
            reportView.nextStep(),
            reportView.growthRecorded()
        );
    }

    public SessionReportResponse getSessionReport(Long sessionId, Long userId) {
        LearningReportView reportView = buildLearningReportView(sessionId, userId);
        PracticeFeedbackReport feedbackReport = reportView.feedbackReport();
        List<StepEvidenceResponse> stepEvidence = evidenceRepository.findByTaskId(reportView.taskId()).stream()
            .limit(20)
            .map(this::toStepEvidenceResponse)
            .toList();

        return new SessionReportResponse(
            reportView.sessionId(),
            reportView.taskId(),
            reportView.nodeId(),
            reportView.nodeName(),
            reportView.stageCode(),
            reportView.stageLabel(),
            reportView.overallScore(),
            reportView.overallAccuracy(),
            reportView.correctCount(),
            reportView.questionCount(),
            reportView.diagnosisSummary(),
            feedbackReport != null && feedbackReport.getDiagnosisSummary() != null && !feedbackReport.getDiagnosisSummary().isBlank()
                ? feedbackReport.getDiagnosisSummary()
                : reportView.diagnosisSummary(),
            reportView.strengths(),
            reportView.weaknesses(),
            reportView.reviewFocus(),
            reportView.questionResults(),
            reportView.weakPoints(),
            stepEvidence,
            feedbackReport == null ? null : feedbackReport.getNextRoundAdvice(),
            feedbackReport == null ? reportView.nextStep().recommendedAction() : feedbackReport.getRecommendedAction(),
            feedbackReport == null ? reportView.nextStep().recommendedAction() : feedbackReport.getRecommendedAction(),
            feedbackReport == null ? null : feedbackReport.getSelectedAction(),
            reportView.nextStep(),
            reportView.growthRecorded(),
            feedbackReport == null ? "AGGREGATED" : feedbackReport.getSource()
        );
    }

    public GrowthDashboardResponse getGrowthDashboard(Long sessionId, Long userId) {
        LearningSession session = requireSession(sessionId, userId);
        List<ConceptNode> nodes = conceptNodeRepository.findByChapterIdOrderByOrderNoAsc(session.getChapterId());
        List<NodeMastery> masteryRows = nodeMasteryRepository.findByUserIdAndChapterId(userId, session.getChapterId());
        Map<Long, NodeMastery> masteryMap = new LinkedHashMap<>();
        for (NodeMastery row : masteryRows) {
            masteryMap.put(row.getNodeId(), row);
        }
        WeakPointDiagnosisService.WeakPointDiagnosisResult diagnosis = weakPointDiagnosisService.diagnoseWeakPoints(sessionId, userId);
        Task trainingTask = resolveTrainingTask(session);
        ConceptNode currentNode = resolveNode(session.getCurrentNodeId())
            .or(() -> resolveNode(trainingTask.getNodeId()))
            .orElseGet(() -> buildFallbackNode(trainingTask.getNodeId()));
        RecommendationView recommendation = buildRecommendation(
            session,
            currentNode,
            masteryMap.get(currentNode.getId()),
            practiceSubmissionRepository.findBySessionIdAndTaskIdAndUserPk(sessionId, trainingTask.getId(), userId),
            diagnosis.weakNodes()
        );

        List<TrainingAttemptSummary> attempts = taskRepository.findRecentTrainingAttempts(sessionId, 5);
        List<String> topErrorTags = attempts.stream()
            .flatMap(item -> item.errorTags().stream())
            .distinct()
            .limit(5)
            .toList();
        BigDecimal averageScore = attempts.isEmpty()
            ? BigDecimal.ZERO
            : BigDecimal.valueOf(attempts.stream()
                .map(TrainingAttemptSummary::score)
                .filter(java.util.Objects::nonNull)
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0D))
                .setScale(2, RoundingMode.HALF_UP);

        int learnedNodeCount = (int) masteryRows.stream().filter(this::isLearned).count();
        int masteredNodeCount = (int) masteryRows.stream().filter(this::isMastered).count();
        BigDecimal averageMasteryScore = masteryRows.isEmpty()
            ? BigDecimal.ZERO
            : masteryRows.stream()
                .map(item -> defaultDecimal(item.getMasteryScore()))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(masteryRows.size()), 2, RoundingMode.HALF_UP);

        return new GrowthDashboardResponse(
            sessionId,
            session.getCourseId(),
            session.getChapterId(),
            learnedNodeCount,
            masteredNodeCount,
            averageMasteryScore,
            currentNode.getId(),
            currentNode.getName(),
            session.getCurrentStage() == null ? null : session.getCurrentStage().name(),
            stageLabel(session.getCurrentStage()),
            diagnosis.weakNodes().stream()
                .limit(5)
                .map(item -> item.nodeName() + ": " + summarizeWeakReason(item.reasons()))
                .toList(),
            new GrowthDashboardRecentPerformanceResponse(
                attempts.size(),
                averageScore,
                attempts.isEmpty() ? null : attempts.get(0).score(),
                topErrorTags
            ),
            recommendation.toResponse(),
            buildGrowthNodes(nodes, masteryMap, session.getCurrentNodeId(), recommendation.targetNodeId())
        );
    }

    private LearningSession requireSession(Long sessionId, Long userId) {
        return sessionRepository.findByIdAndUserPk(sessionId, userId)
            .orElseThrow(() -> new NotFoundException(NOT_FOUND_MESSAGE));
    }

    private LearningReportView buildLearningReportView(Long sessionId, Long userId) {
        LearningSession session = requireSession(sessionId, userId);
        Task trainingTask = resolveTrainingTask(session);
        ConceptNode node = conceptNodeRepository.findById(trainingTask.getNodeId())
            .orElseGet(() -> buildFallbackNode(trainingTask.getNodeId()));
        Optional<PracticeQuiz> quiz = practiceQuizRepository.findLatestBySessionIdAndTaskIdAndUserPk(sessionId, trainingTask.getId(), userId);
        PracticeFeedbackReport report = quiz.flatMap(item -> practiceFeedbackReportRepository.findByQuizId(item.getId())).orElse(null);
        List<PracticeItem> items = quiz.map(item -> practiceRepository.findByQuizId(item.getId()))
            .orElseGet(() -> practiceRepository.findBySessionIdAndTaskIdAndUserPk(sessionId, trainingTask.getId(), userId));
        List<PracticeSubmission> submissions = practiceSubmissionRepository.findBySessionIdAndTaskIdAndUserPk(sessionId, trainingTask.getId(), userId);
        NodeMastery mastery = nodeMasteryRepository.findByUserIdAndNodeId(userId, trainingTask.getNodeId()).orElse(null);
        WeakPointDiagnosisService.WeakPointDiagnosisResult diagnosis = weakPointDiagnosisService.diagnoseWeakPoints(sessionId, userId);
        RecommendationView recommendation = buildRecommendation(session, node, mastery, submissions, diagnosis.weakNodes());

        int questionCount = items.isEmpty() ? submissions.size() : items.size();
        int correctCount = (int) submissions.stream().filter(item -> Boolean.TRUE.equals(item.getCorrect())).count();
        Integer overallScore = submissions.isEmpty() ? null : Math.round((float) submissions.stream()
            .map(PracticeSubmission::getScore)
            .filter(java.util.Objects::nonNull)
            .mapToInt(Integer::intValue)
            .sum() / submissions.size());
        BigDecimal overallAccuracy = questionCount == 0
            ? BigDecimal.ZERO
            : BigDecimal.valueOf(correctCount)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(questionCount), 2, RoundingMode.HALF_UP);

        return new LearningReportView(
            sessionId,
            trainingTask.getId(),
            node.getId(),
            node.getName(),
            trainingTask.getStage().name(),
            stageLabel(trainingTask.getStage()),
            overallScore,
            overallAccuracy,
            correctCount,
            questionCount,
            report != null ? report.getDiagnosisSummary() : fallbackDiagnosis(overallAccuracy),
            report != null ? readStringList(report.getStrengthsJson()) : fallbackStrengths(correctCount, questionCount),
            report != null ? readStringList(report.getWeaknessesJson()) : fallbackWeaknesses(submissions),
            report != null ? readStringList(report.getReviewFocusJson()) : fallbackReviewFocus(diagnosis.weakNodes()),
            toMasteryResponse(mastery, node),
            buildQuestionResults(items, submissions),
            diagnosis.weakNodes().stream().limit(5).map(this::toWeakPointResponse).toList(),
            recommendation.toResponse(),
            mastery != null,
            report
        );
    }

    private Task resolveTrainingTask(LearningSession session) {
        List<Task> tasks = taskRepository.findBySessionIdWithStatus(session.getId());
        Comparator<Task> comparator = Comparator
            .comparing(Task::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(Task::getId, Comparator.nullsLast(Comparator.naturalOrder()))
            .reversed();

        return tasks.stream()
            .filter(item -> item.getStage() == Stage.TRAINING)
            .filter(item -> session.getCurrentNodeId() == null || session.getCurrentNodeId().equals(item.getNodeId()))
            .sorted(comparator)
            .findFirst()
            .or(() -> tasks.stream().filter(item -> item.getStage() == Stage.TRAINING).sorted(comparator).findFirst())
            .orElseThrow(() -> new NotFoundException(NOT_FOUND_MESSAGE));
    }

    private List<LearningReportQuestionResponse> buildQuestionResults(List<PracticeItem> items, List<PracticeSubmission> submissions) {
        Map<Long, PracticeSubmission> submissionMap = new LinkedHashMap<>();
        for (PracticeSubmission submission : submissions) {
            submissionMap.put(submission.getPracticeItemId(), submission);
        }
        if (items.isEmpty()) {
            return submissions.stream()
                .map(item -> new LearningReportQuestionResponse(
                    item.getPracticeItemId(),
                    null,
                    null,
                    item.getScore(),
                    item.getCorrect(),
                    item.getFeedback(),
                    readStringList(item.getErrorTagsJson())
                ))
                .toList();
        }
        return items.stream()
            .map(item -> {
                PracticeSubmission submission = submissionMap.get(item.getId());
                return new LearningReportQuestionResponse(
                    item.getId(),
                    item.getQuestionType() == null ? null : item.getQuestionType().name(),
                    item.getStem(),
                    submission == null ? null : submission.getScore(),
                    submission == null ? null : submission.getCorrect(),
                    submission == null ? null : submission.getFeedback(),
                    submission == null ? List.of() : readStringList(submission.getErrorTagsJson())
                );
            })
            .toList();
    }

    private LearningReportMasteryResponse toMasteryResponse(NodeMastery mastery, ConceptNode node) {
        if (mastery == null) {
            return new LearningReportMasteryResponse(node.getId(), node.getName(), BigDecimal.ZERO, BigDecimal.ZERO, null, "NOT_STARTED");
        }
        return new LearningReportMasteryResponse(
            mastery.getNodeId(),
            mastery.getNodeName() == null || mastery.getNodeName().isBlank() ? node.getName() : mastery.getNodeName(),
            defaultDecimal(mastery.getMasteryScore()),
            defaultDecimal(mastery.getTrainingAccuracy()),
            mastery.getLatestEvaluationScore(),
            masteryStatus(mastery)
        );
    }

    private WeakPointNodeResponse toWeakPointResponse(WeakPointDiagnosisService.WeakNode node) {
        return new WeakPointNodeResponse(
            node.nodeId(),
            node.nodeName(),
            node.masteryScore(),
            node.trainingAccuracy(),
            node.latestEvaluationScore(),
            node.attemptCount(),
            node.recentErrorTags(),
            node.reasons()
        );
    }

    private List<GrowthDashboardNodeResponse> buildGrowthNodes(
        List<ConceptNode> nodes,
        Map<Long, NodeMastery> masteryMap,
        Long currentNodeId,
        Long recommendedNodeId
    ) {
        return nodes.stream()
            .map(node -> {
                NodeMastery mastery = masteryMap.get(node.getId());
                return new GrowthDashboardNodeResponse(
                    node.getId(),
                    node.getName(),
                    mastery == null ? BigDecimal.ZERO : defaultDecimal(mastery.getMasteryScore()),
                    masteryStatus(mastery),
                    node.getId().equals(currentNodeId),
                    node.getId().equals(recommendedNodeId)
                );
            })
            .toList();
    }

    private RecommendationView buildRecommendation(
        LearningSession session,
        ConceptNode currentNode,
        NodeMastery currentMastery,
        List<PracticeSubmission> submissions,
        List<WeakPointDiagnosisService.WeakNode> weakNodes
    ) {
        BigDecimal masteryScore = currentMastery == null ? BigDecimal.ZERO : defaultDecimal(currentMastery.getMasteryScore());
        Integer averageScore = submissions.isEmpty() ? null : Math.round((float) submissions.stream()
            .map(PracticeSubmission::getScore)
            .filter(java.util.Objects::nonNull)
            .mapToInt(Integer::intValue)
            .sum() / submissions.size());
        boolean currentNodeWeak = weakNodes.stream().anyMatch(item -> item.nodeId().equals(currentNode.getId()));
        List<ConceptNode> nodes = conceptNodeRepository.findByChapterIdOrderByOrderNoAsc(session.getChapterId());
        int index = findNodeIndex(nodes, currentNode.getId());
        ConceptNode previous = index > 0 ? nodes.get(index - 1) : currentNode;
        ConceptNode next = index >= 0 && index + 1 < nodes.size() ? nodes.get(index + 1) : currentNode;

        if ((averageScore != null && averageScore < 50) || masteryScore.compareTo(BigDecimal.valueOf(45)) < 0) {
            return new RecommendationView(
                "BACKTRACK",
                currentNode.getId().equals(previous.getId())
                    ? "当前节点基础还不稳，先继续补基础并重做一轮训练。"
                    : "当前训练表现偏弱，建议先回到前置节点补基础，再回来训练。",
                previous.getId(),
                previous.getName(),
                "TRAINING",
                new BigDecimal("0.91")
            );
        }
        if ((averageScore != null && averageScore < 75) || masteryScore.compareTo(BigDecimal.valueOf(65)) < 0 || currentNodeWeak) {
            return new RecommendationView(
                "REINFORCE",
                "当前节点还有明显薄弱点，建议继续在本节点做一轮针对性强化训练。",
                currentNode.getId(),
                currentNode.getName(),
                "TRAINING",
                new BigDecimal("0.82")
            );
        }
        if ((averageScore != null && averageScore < 85) || masteryScore.compareTo(BigDecimal.valueOf(80)) < 0) {
            return new RecommendationView(
                "REVIEW",
                "整体表现基本达标，但还需要一次轻量复习来稳定掌握度。",
                currentNode.getId(),
                currentNode.getName(),
                "REFLECTION",
                new BigDecimal("0.72")
            );
        }
        return new RecommendationView(
            "ADVANCE",
            next.getId().equals(currentNode.getId())
                ? "当前节点表现稳定，可以进入评估反思并准备结束本章。"
                : "当前节点表现稳定，可以推进到下一个知识点继续学习。",
            next.getId(),
            next.getName(),
            "STRUCTURE",
            new BigDecimal("0.88")
        );
    }

    private int findNodeIndex(List<ConceptNode> nodes, Long nodeId) {
        for (int i = 0; i < nodes.size(); i++) {
            if (nodes.get(i).getId().equals(nodeId)) {
                return i;
            }
        }
        return -1;
    }

    private Optional<ConceptNode> resolveNode(Long nodeId) {
        if (nodeId == null) {
            return Optional.empty();
        }
        return conceptNodeRepository.findById(nodeId);
    }

    private ConceptNode buildFallbackNode(Long nodeId) {
        ConceptNode node = new ConceptNode();
        node.setId(nodeId);
        node.setName("Current Node");
        return node;
    }

    private List<String> readStringList(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            JsonNode parsed = objectMapper.readTree(json);
            if (!parsed.isArray()) {
                return List.of();
            }
            List<String> values = new ArrayList<>();
            for (JsonNode item : parsed) {
                if (item.isTextual() && !item.asText().isBlank()) {
                    values.add(item.asText().trim());
                }
            }
            return values;
        } catch (Exception ex) {
            return List.of();
        }
    }

    private String fallbackDiagnosis(BigDecimal overallAccuracy) {
        if (overallAccuracy.compareTo(BigDecimal.valueOf(80)) >= 0) {
            return "本轮训练完成度较高，可以继续推进到下一步。";
        }
        if (overallAccuracy.compareTo(BigDecimal.valueOf(60)) >= 0) {
            return "本轮训练基本完成，但还有部分薄弱点需要补强。";
        }
        return "本轮训练暴露出较明显的基础问题，建议先补基础再推进。";
    }

    private List<String> fallbackStrengths(int correctCount, int questionCount) {
        if (questionCount == 0) {
            return List.of("已建立当前节点的训练记录。");
        }
        if (correctCount > 0) {
            return List.of("至少有一部分题目已经答对，说明核心思路开始建立。");
        }
        return List.of("已完成本轮训练提交，可以基于结果继续收敛薄弱点。");
    }

    private List<String> fallbackWeaknesses(List<PracticeSubmission> submissions) {
        return submissions.stream()
            .filter(item -> Boolean.FALSE.equals(item.getCorrect()))
            .map(PracticeSubmission::getFeedback)
            .filter(item -> item != null && !item.isBlank())
            .limit(3)
            .toList();
    }

    private List<String> fallbackReviewFocus(List<WeakPointDiagnosisService.WeakNode> weakNodes) {
        return weakNodes.stream()
            .flatMap(item -> item.recentErrorTags().stream())
            .distinct()
            .limit(3)
            .toList();
    }

    private StepEvidenceResponse toStepEvidenceResponse(Evidence evidence) {
        return new StepEvidenceResponse(
            evidence.getId(),
            evidence.getStepId(),
            evidence.getStepIndex(),
            evidence.getEvidenceType(),
            summarizeEvidence(evidence.getContentJson()),
            evidence.getCreatedAt()
        );
    }

    private String summarizeEvidence(String contentJson) {
        if (contentJson == null || contentJson.isBlank()) {
            return "暂无证据摘要";
        }
        try {
            JsonNode node = objectMapper.readTree(contentJson);
            Integer score = node.path("score").isNumber() ? node.path("score").asInt() : null;
            String diagnosis = node.path("feedback_json").path("diagnosis").asText("");
            if (diagnosis.isBlank()) {
                diagnosis = node.path("feedback").asText("");
            }
            List<String> tags = readStringList(node.path("error_tags").toString());
            String scoreText = score == null ? null : ("得分 " + score);
            String tagText = tags.isEmpty() ? null : ("标签 " + String.join(" / ", tags.stream().limit(2).toList()));
            String diagnosisText = diagnosis.isBlank() ? null : diagnosis;
            return java.util.stream.Stream.of(scoreText, tagText, diagnosisText)
                .filter(item -> item != null && !item.isBlank())
                .limit(3)
                .reduce((a, b) -> a + "；" + b)
                .orElse("已记录步骤证据");
        } catch (Exception ex) {
            return "已记录步骤证据";
        }
    }

    private String masteryStatus(NodeMastery mastery) {
        if (mastery == null || !isLearned(mastery)) {
            return "NOT_STARTED";
        }
        BigDecimal score = defaultDecimal(mastery.getMasteryScore());
        if (score.compareTo(BigDecimal.valueOf(80)) >= 0) {
            return "MASTERED";
        }
        if (score.compareTo(BigDecimal.valueOf(60)) >= 0) {
            return "LEARNING";
        }
        return "NEEDS_REINFORCEMENT";
    }

    private boolean isLearned(NodeMastery mastery) {
        return mastery != null && ((mastery.getAttemptCount() != null && mastery.getAttemptCount() > 0)
            || defaultDecimal(mastery.getMasteryScore()).compareTo(BigDecimal.ZERO) > 0);
    }

    private boolean isMastered(NodeMastery mastery) {
        return mastery != null && defaultDecimal(mastery.getMasteryScore()).compareTo(BigDecimal.valueOf(80)) >= 0;
    }

    private BigDecimal defaultDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String stageLabel(Stage stage) {
        if (stage == null) {
            return null;
        }
        return switch (stage) {
            case STRUCTURE -> "建立框架";
            case UNDERSTANDING -> "理解原理";
            case TRAINING -> "训练应用";
            case REFLECTION -> "评估反思";
        };
    }

    private String summarizeWeakReason(List<String> reasons) {
        if (reasons == null || reasons.isEmpty()) {
            return "需要继续观察";
        }
        return reasons.get(0);
    }

    record RecommendationView(
        String recommendedAction,
        String reason,
        Long targetNodeId,
        String targetNodeName,
        String targetTaskType,
        BigDecimal confidence
    ) {
        NextStepRecommendationResponse toResponse() {
            return new NextStepRecommendationResponse(
                recommendedAction,
                reason,
                targetNodeId,
                targetNodeName,
                targetTaskType,
                confidence
            );
        }
    }

    record LearningReportView(
        Long sessionId,
        Long taskId,
        Long nodeId,
        String nodeName,
        String stageCode,
        String stageLabel,
        Integer overallScore,
        BigDecimal overallAccuracy,
        Integer correctCount,
        Integer questionCount,
        String diagnosisSummary,
        List<String> strengths,
        List<String> weaknesses,
        List<String> reviewFocus,
        LearningReportMasteryResponse mastery,
        List<LearningReportQuestionResponse> questionResults,
        List<WeakPointNodeResponse> weakPoints,
        NextStepRecommendationResponse nextStep,
        boolean growthRecorded,
        PracticeFeedbackReport feedbackReport
    ) {
    }
}
