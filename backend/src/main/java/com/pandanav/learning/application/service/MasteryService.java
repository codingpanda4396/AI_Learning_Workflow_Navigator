package com.pandanav.learning.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.pandanav.learning.domain.enums.Stage;
import com.pandanav.learning.domain.model.LearningEvent;
import com.pandanav.learning.domain.model.LearningSession;
import com.pandanav.learning.domain.model.Mastery;
import com.pandanav.learning.domain.model.NodeMastery;
import com.pandanav.learning.domain.model.PracticeSubmission;
import com.pandanav.learning.domain.model.Task;
import com.pandanav.learning.domain.repository.LearningEventRepository;
import com.pandanav.learning.domain.repository.MasteryRepository;
import com.pandanav.learning.domain.repository.NodeMasteryRepository;
import com.pandanav.learning.domain.repository.PracticeSubmissionRepository;
import com.pandanav.learning.domain.repository.SessionRepository;
import com.pandanav.learning.domain.repository.TaskRepository;
import com.pandanav.learning.infrastructure.exception.ConflictException;
import com.pandanav.learning.infrastructure.exception.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class MasteryService {

    private static final String NOT_FOUND_MESSAGE = "Session or task not found.";

    private final SessionRepository sessionRepository;
    private final TaskRepository taskRepository;
    private final PracticeSubmissionRepository practiceSubmissionRepository;
    private final NodeMasteryRepository nodeMasteryRepository;
    private final MasteryRepository masteryRepository;
    private final LearningEventRepository learningEventRepository;
    private final ObjectMapper objectMapper;

    public MasteryService(
        SessionRepository sessionRepository,
        TaskRepository taskRepository,
        PracticeSubmissionRepository practiceSubmissionRepository,
        NodeMasteryRepository nodeMasteryRepository,
        MasteryRepository masteryRepository,
        LearningEventRepository learningEventRepository,
        ObjectMapper objectMapper
    ) {
        this.sessionRepository = sessionRepository;
        this.taskRepository = taskRepository;
        this.practiceSubmissionRepository = practiceSubmissionRepository;
        this.nodeMasteryRepository = nodeMasteryRepository;
        this.masteryRepository = masteryRepository;
        this.learningEventRepository = learningEventRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public NodeMastery recalculateNodeMastery(Long sessionId, Long taskId, Long userId) {
        LearningSession session = sessionRepository.findByIdAndUserPk(sessionId, userId)
            .orElseThrow(() -> new NotFoundException(NOT_FOUND_MESSAGE));
        Task task = taskRepository.findByIdAndUserPk(taskId, userId)
            .orElseThrow(() -> new NotFoundException(NOT_FOUND_MESSAGE));
        if (!task.getSessionId().equals(sessionId)) {
            throw new NotFoundException(NOT_FOUND_MESSAGE);
        }
        if (task.getStage() != Stage.TRAINING) {
            throw new ConflictException("Mastery recalculation is only allowed for TRAINING stage tasks.");
        }

        List<PracticeSubmission> submissions = practiceSubmissionRepository.findBySessionIdAndTaskIdAndUserPk(sessionId, taskId, userId);
        int attemptCount = submissions.size();
        long correctCount = submissions.stream().map(PracticeSubmission::getCorrect).filter(Boolean.TRUE::equals).count();
        BigDecimal trainingAccuracy = attemptCount == 0
            ? BigDecimal.ZERO
            : BigDecimal.valueOf(correctCount)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(attemptCount), 4, RoundingMode.HALF_UP);

        Integer latestEvaluationScore = taskRepository.findLatestScoreByTaskId(taskId).orElse(null);
        Map<String, Integer> errorFrequency = collectErrorFrequency(submissions);
        List<String> recentErrorTags = topErrorTags(errorFrequency, 6);
        int repeatedTagKinds = (int) errorFrequency.values().stream().filter(v -> v >= 2).count();
        int consecutiveWrong = countConsecutiveWrong(submissions);

        BigDecimal masteryScore = calculateMasteryScore(
            trainingAccuracy,
            latestEvaluationScore,
            repeatedTagKinds,
            consecutiveWrong,
            attemptCount
        );

        NodeMastery row = new NodeMastery();
        row.setUserId(userId);
        row.setSessionId(sessionId);
        row.setNodeId(task.getNodeId());
        row.setMasteryScore(masteryScore);
        row.setTrainingAccuracy(trainingAccuracy.setScale(2, RoundingMode.HALF_UP));
        row.setRecentErrorTagsJson(toJson(recentErrorTags));
        row.setLatestEvaluationScore(latestEvaluationScore);
        row.setAttemptCount(attemptCount);

        NodeMastery saved = nodeMasteryRepository.upsert(row);
        syncLegacyMastery(session, saved);
        emitMasteryEvent(sessionId, userId, taskId, saved);
        return saved;
    }

    public NodeMastery getNodeMastery(Long sessionId, Long userId, Long nodeId) {
        sessionRepository.findByIdAndUserPk(sessionId, userId)
            .orElseThrow(() -> new NotFoundException(NOT_FOUND_MESSAGE));
        return nodeMasteryRepository.findByUserIdAndNodeId(userId, nodeId)
            .orElseThrow(() -> new NotFoundException("Node mastery not found."));
    }

    private BigDecimal calculateMasteryScore(
        BigDecimal trainingAccuracy,
        Integer latestEvaluationScore,
        int repeatedTagKinds,
        int consecutiveWrong,
        int attemptCount
    ) {
        BigDecimal evaluation = latestEvaluationScore == null
            ? BigDecimal.valueOf(50)
            : BigDecimal.valueOf(latestEvaluationScore);

        BigDecimal score = trainingAccuracy.multiply(BigDecimal.valueOf(0.6))
            .add(evaluation.multiply(BigDecimal.valueOf(0.3)))
            .add(BigDecimal.valueOf(10));

        score = score
            .subtract(BigDecimal.valueOf(Math.min(3, consecutiveWrong) * 8L))
            .subtract(BigDecimal.valueOf(Math.min(3, repeatedTagKinds) * 4L));

        if (attemptCount >= 3 && trainingAccuracy.compareTo(BigDecimal.valueOf(80)) >= 0) {
            score = score.add(BigDecimal.valueOf(5));
        }

        if (score.compareTo(BigDecimal.ZERO) < 0) {
            score = BigDecimal.ZERO;
        }
        if (score.compareTo(BigDecimal.valueOf(100)) > 0) {
            score = BigDecimal.valueOf(100);
        }
        return score.setScale(2, RoundingMode.HALF_UP);
    }

    private Map<String, Integer> collectErrorFrequency(List<PracticeSubmission> submissions) {
        Map<String, Integer> freq = new LinkedHashMap<>();
        for (PracticeSubmission submission : submissions) {
            for (String tag : parseErrorTags(submission.getErrorTagsJson())) {
                freq.merge(tag, 1, Integer::sum);
            }
        }
        return freq;
    }

    private List<String> topErrorTags(Map<String, Integer> freq, int limit) {
        return freq.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder())
                .thenComparing(Map.Entry::getKey))
            .limit(limit)
            .map(Map.Entry::getKey)
            .toList();
    }

    private int countConsecutiveWrong(List<PracticeSubmission> submissions) {
        int count = 0;
        for (PracticeSubmission submission : submissions) {
            if (Boolean.TRUE.equals(submission.getCorrect())) {
                break;
            }
            count++;
        }
        return count;
    }

    private List<String> parseErrorTags(String errorTagsJson) {
        if (errorTagsJson == null || errorTagsJson.isBlank()) {
            return List.of();
        }
        try {
            JsonNode node = objectMapper.readTree(errorTagsJson);
            if (!node.isArray()) {
                return List.of();
            }
            List<String> tags = new ArrayList<>();
            for (JsonNode item : node) {
                if (item.isTextual() && !item.asText().isBlank()) {
                    tags.add(item.asText().trim());
                }
            }
            return tags;
        } catch (Exception ex) {
            return List.of();
        }
    }

    private void syncLegacyMastery(LearningSession session, NodeMastery saved) {
        Mastery legacy = new Mastery();
        legacy.setUserId(session.getUserId());
        legacy.setNodeId(saved.getNodeId());
        legacy.setMasteryValue(saved.getMasteryScore()
            .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP)
            .setScale(3, RoundingMode.HALF_UP));
        masteryRepository.upsert(legacy);
    }

    private void emitMasteryEvent(Long sessionId, Long userId, Long taskId, NodeMastery saved) {
        LearningEvent event = new LearningEvent();
        event.setSessionId(sessionId);
        event.setUserId(userId);
        event.setEventType("NODE_MASTERY_RECALCULATED");
        event.setEventData(toJson(Map.of(
            "task_id", taskId,
            "node_id", saved.getNodeId(),
            "mastery_score", saved.getMasteryScore(),
            "training_accuracy", saved.getTrainingAccuracy(),
            "latest_evaluation_score", saved.getLatestEvaluationScore(),
            "attempt_count", saved.getAttemptCount(),
            "recent_error_tags", saved.getRecentErrorTagsJson()
        )));
        learningEventRepository.save(event);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            return "{}";
        }
    }
}
