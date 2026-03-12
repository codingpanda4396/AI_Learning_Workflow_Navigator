package com.pandanav.learning.domain.model;

import com.pandanav.learning.domain.enums.CapabilityLevel;

import java.time.OffsetDateTime;
import java.util.List;

public class CapabilityProfile {

    private Long id;
    private Long learningSessionId;
    private Long userPk;
    private Long sourceDiagnosisId;
    private CapabilityLevel currentLevel;
    private List<String> strengths;
    private List<String> weaknesses;
    private String learningPreference;
    private String timeBudget;
    private String goalOrientation;
    private String summaryText;
    private String planExplanation;
    private Integer version;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getLearningSessionId() {
        return learningSessionId;
    }

    public void setLearningSessionId(Long learningSessionId) {
        this.learningSessionId = learningSessionId;
    }

    public Long getUserPk() {
        return userPk;
    }

    public void setUserPk(Long userPk) {
        this.userPk = userPk;
    }

    public Long getSourceDiagnosisId() {
        return sourceDiagnosisId;
    }

    public void setSourceDiagnosisId(Long sourceDiagnosisId) {
        this.sourceDiagnosisId = sourceDiagnosisId;
    }

    public CapabilityLevel getCurrentLevel() {
        return currentLevel;
    }

    public void setCurrentLevel(CapabilityLevel currentLevel) {
        this.currentLevel = currentLevel;
    }

    public List<String> getStrengths() {
        return strengths;
    }

    public void setStrengths(List<String> strengths) {
        this.strengths = strengths;
    }

    public List<String> getWeaknesses() {
        return weaknesses;
    }

    public void setWeaknesses(List<String> weaknesses) {
        this.weaknesses = weaknesses;
    }

    public String getLearningPreference() {
        return learningPreference;
    }

    public void setLearningPreference(String learningPreference) {
        this.learningPreference = learningPreference;
    }

    public String getTimeBudget() {
        return timeBudget;
    }

    public void setTimeBudget(String timeBudget) {
        this.timeBudget = timeBudget;
    }

    public String getGoalOrientation() {
        return goalOrientation;
    }

    public void setGoalOrientation(String goalOrientation) {
        this.goalOrientation = goalOrientation;
    }

    public String getSummaryText() {
        return summaryText;
    }

    public void setSummaryText(String summaryText) {
        this.summaryText = summaryText;
    }

    public String getPlanExplanation() {
        return planExplanation;
    }

    public void setPlanExplanation(String planExplanation) {
        this.planExplanation = planExplanation;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
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
