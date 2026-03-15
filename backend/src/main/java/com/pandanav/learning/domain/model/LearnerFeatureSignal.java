package com.pandanav.learning.domain.model;

import java.time.OffsetDateTime;
import java.util.Map;

public class LearnerFeatureSignal {

    private Long id;
    private Long diagnosisSessionId;
    private Long learningSessionId;
    private Long userId;
    private String questionId;
    private String featureKey;
    private String featureValue;
    private double scoreDelta;
    private double confidence;
    private Map<String, Object> evidence;
    private String source;
    private OffsetDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDiagnosisSessionId() {
        return diagnosisSessionId;
    }

    public void setDiagnosisSessionId(Long diagnosisSessionId) {
        this.diagnosisSessionId = diagnosisSessionId;
    }

    public Long getLearningSessionId() {
        return learningSessionId;
    }

    public void setLearningSessionId(Long learningSessionId) {
        this.learningSessionId = learningSessionId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getQuestionId() {
        return questionId;
    }

    public void setQuestionId(String questionId) {
        this.questionId = questionId;
    }

    public String getFeatureKey() {
        return featureKey;
    }

    public void setFeatureKey(String featureKey) {
        this.featureKey = featureKey;
    }

    public String getFeatureValue() {
        return featureValue;
    }

    public void setFeatureValue(String featureValue) {
        this.featureValue = featureValue;
    }

    public double getScoreDelta() {
        return scoreDelta;
    }

    public void setScoreDelta(double scoreDelta) {
        this.scoreDelta = scoreDelta;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public Map<String, Object> getEvidence() {
        return evidence;
    }

    public void setEvidence(Map<String, Object> evidence) {
        this.evidence = evidence;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
