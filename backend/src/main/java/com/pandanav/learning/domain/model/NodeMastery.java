package com.pandanav.learning.domain.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class NodeMastery {

    private Long id;
    private Long userId;
    private Long sessionId;
    private Long nodeId;
    private String nodeName;
    private BigDecimal masteryScore;
    private BigDecimal trainingAccuracy;
    private String recentErrorTagsJson;
    private Integer latestEvaluationScore;
    private Integer attemptCount;
    private OffsetDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public Long getNodeId() {
        return nodeId;
    }

    public void setNodeId(Long nodeId) {
        this.nodeId = nodeId;
    }

    public String getNodeName() {
        return nodeName;
    }

    public void setNodeName(String nodeName) {
        this.nodeName = nodeName;
    }

    public BigDecimal getMasteryScore() {
        return masteryScore;
    }

    public void setMasteryScore(BigDecimal masteryScore) {
        this.masteryScore = masteryScore;
    }

    public BigDecimal getTrainingAccuracy() {
        return trainingAccuracy;
    }

    public void setTrainingAccuracy(BigDecimal trainingAccuracy) {
        this.trainingAccuracy = trainingAccuracy;
    }

    public String getRecentErrorTagsJson() {
        return recentErrorTagsJson;
    }

    public void setRecentErrorTagsJson(String recentErrorTagsJson) {
        this.recentErrorTagsJson = recentErrorTagsJson;
    }

    public Integer getLatestEvaluationScore() {
        return latestEvaluationScore;
    }

    public void setLatestEvaluationScore(Integer latestEvaluationScore) {
        this.latestEvaluationScore = latestEvaluationScore;
    }

    public Integer getAttemptCount() {
        return attemptCount;
    }

    public void setAttemptCount(Integer attemptCount) {
        this.attemptCount = attemptCount;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
