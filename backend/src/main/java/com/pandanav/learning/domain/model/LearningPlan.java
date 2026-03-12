package com.pandanav.learning.domain.model;

import com.pandanav.learning.domain.enums.LearningPlanStatus;

import java.time.OffsetDateTime;

public class LearningPlan {

    private Long id;
    private Long userId;
    private String goalId;
    private String diagnosisId;
    private Long sessionId;
    private LearningPlanStatus status;
    private String summaryJson;
    private String reasonsJson;
    private String focusesJson;
    private String pathPreviewJson;
    private String taskPreviewJson;
    private String adjustmentsJson;
    private String planningContextJson;
    private String llmTraceId;
    private OffsetDateTime createdAt;
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

    public String getGoalId() {
        return goalId;
    }

    public void setGoalId(String goalId) {
        this.goalId = goalId;
    }

    public String getDiagnosisId() {
        return diagnosisId;
    }

    public void setDiagnosisId(String diagnosisId) {
        this.diagnosisId = diagnosisId;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public LearningPlanStatus getStatus() {
        return status;
    }

    public void setStatus(LearningPlanStatus status) {
        this.status = status;
    }

    public String getSummaryJson() {
        return summaryJson;
    }

    public void setSummaryJson(String summaryJson) {
        this.summaryJson = summaryJson;
    }

    public String getReasonsJson() {
        return reasonsJson;
    }

    public void setReasonsJson(String reasonsJson) {
        this.reasonsJson = reasonsJson;
    }

    public String getFocusesJson() {
        return focusesJson;
    }

    public void setFocusesJson(String focusesJson) {
        this.focusesJson = focusesJson;
    }

    public String getPathPreviewJson() {
        return pathPreviewJson;
    }

    public void setPathPreviewJson(String pathPreviewJson) {
        this.pathPreviewJson = pathPreviewJson;
    }

    public String getTaskPreviewJson() {
        return taskPreviewJson;
    }

    public void setTaskPreviewJson(String taskPreviewJson) {
        this.taskPreviewJson = taskPreviewJson;
    }

    public String getAdjustmentsJson() {
        return adjustmentsJson;
    }

    public void setAdjustmentsJson(String adjustmentsJson) {
        this.adjustmentsJson = adjustmentsJson;
    }

    public String getPlanningContextJson() {
        return planningContextJson;
    }

    public void setPlanningContextJson(String planningContextJson) {
        this.planningContextJson = planningContextJson;
    }

    public String getLlmTraceId() {
        return llmTraceId;
    }

    public void setLlmTraceId(String llmTraceId) {
        this.llmTraceId = llmTraceId;
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
