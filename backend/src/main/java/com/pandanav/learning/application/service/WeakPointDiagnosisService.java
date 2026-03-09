package com.pandanav.learning.application.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pandanav.learning.domain.model.LearningSession;
import com.pandanav.learning.domain.model.NodeMastery;
import com.pandanav.learning.domain.repository.NodeMasteryRepository;
import com.pandanav.learning.domain.repository.SessionRepository;
import com.pandanav.learning.infrastructure.exception.NotFoundException;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class WeakPointDiagnosisService {

    private static final String NOT_FOUND_MESSAGE = "Session or task not found.";

    private final SessionRepository sessionRepository;
    private final NodeMasteryRepository nodeMasteryRepository;
    private final ObjectMapper objectMapper;

    public WeakPointDiagnosisService(
        SessionRepository sessionRepository,
        NodeMasteryRepository nodeMasteryRepository,
        ObjectMapper objectMapper
    ) {
        this.sessionRepository = sessionRepository;
        this.nodeMasteryRepository = nodeMasteryRepository;
        this.objectMapper = objectMapper;
    }

    public WeakPointDiagnosisResult diagnoseWeakPoints(Long sessionId, Long userId) {
        LearningSession session = sessionRepository.findByIdAndUserPk(sessionId, userId)
            .orElseThrow(() -> new NotFoundException(NOT_FOUND_MESSAGE));

        List<NodeMastery> rows = nodeMasteryRepository.findByUserIdAndChapterId(userId, session.getChapterId());
        List<WeakNode> weakNodes = rows.stream()
            .map(this::toWeakNode)
            .filter(node -> !node.reasons().isEmpty())
            .toList();

        String summary;
        if (weakNodes.isEmpty()) {
            summary = "No obvious weak point detected for current chapter.";
        } else {
            List<String> top = weakNodes.stream().limit(3).map(WeakNode::nodeName).toList();
            summary = "Detected " + weakNodes.size() + " weak nodes. Focus first on: " + String.join(", ", top) + ".";
        }

        return new WeakPointDiagnosisResult(sessionId, userId, summary, weakNodes);
    }

    public record WeakPointDiagnosisResult(
        Long sessionId,
        Long userId,
        String diagnosisSummary,
        List<WeakNode> weakNodes
    ) {
    }

    public record WeakNode(
        Long nodeId,
        String nodeName,
        BigDecimal masteryScore,
        BigDecimal trainingAccuracy,
        Integer latestEvaluationScore,
        Integer attemptCount,
        List<String> recentErrorTags,
        List<String> reasons
    ) {
    }

    private WeakNode toWeakNode(NodeMastery item) {
        BigDecimal masteryScore = item.getMasteryScore() == null ? BigDecimal.ZERO : item.getMasteryScore();
        BigDecimal trainingAccuracy = item.getTrainingAccuracy() == null ? BigDecimal.ZERO : item.getTrainingAccuracy();
        Integer attemptCount = item.getAttemptCount() == null ? 0 : item.getAttemptCount();
        List<String> tags = parseTags(item.getRecentErrorTagsJson());

        List<String> reasons = new ArrayList<>();
        if (masteryScore.compareTo(BigDecimal.valueOf(60)) < 0) {
            reasons.add("LOW_MASTERY_SCORE");
        }
        if (attemptCount >= 2 && trainingAccuracy.compareTo(BigDecimal.valueOf(70)) < 0) {
            reasons.add("LOW_TRAINING_ACCURACY");
        }
        if (tags.size() >= 2) {
            reasons.add("REPEATED_ERROR_TAGS");
        }

        return new WeakNode(
            item.getNodeId(),
            item.getNodeName(),
            masteryScore,
            trainingAccuracy,
            item.getLatestEvaluationScore(),
            attemptCount,
            tags,
            reasons
        );
    }

    private List<String> parseTags(String tagsJson) {
        if (tagsJson == null || tagsJson.isBlank()) {
            return List.of();
        }
        try {
            JsonNode node = objectMapper.readTree(tagsJson);
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
}
