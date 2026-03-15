package com.pandanav.learning.domain.model;

import java.time.OffsetDateTime;
import java.util.Map;

public class LearnerProfileSnapshot {

    private Long id;
    private Long diagnosisSessionId;
    private Long learningSessionId;
    private Long userId;
    private Integer profileVersion;
    private Map<String, Object> featureSummary;
    private Map<String, Object> strategyHints;
    private Map<String, Object> constraints;
    private Map<String, Object> explanations;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

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

    public Integer getProfileVersion() {
        return profileVersion;
    }

    public void setProfileVersion(Integer profileVersion) {
        this.profileVersion = profileVersion;
    }

    public Map<String, Object> getFeatureSummary() {
        return featureSummary;
    }

    public void setFeatureSummary(Map<String, Object> featureSummary) {
        this.featureSummary = featureSummary;
    }

    public Map<String, Object> getStrategyHints() {
        return strategyHints;
    }

    public void setStrategyHints(Map<String, Object> strategyHints) {
        this.strategyHints = strategyHints;
    }

    public Map<String, Object> getConstraints() {
        return constraints;
    }

    public void setConstraints(Map<String, Object> constraints) {
        this.constraints = constraints;
    }

    public Map<String, Object> getExplanations() {
        return explanations;
    }

    public void setExplanations(Map<String, Object> explanations) {
        this.explanations = explanations;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
