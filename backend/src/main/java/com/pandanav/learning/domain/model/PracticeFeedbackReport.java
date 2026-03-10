package com.pandanav.learning.domain.model;

import com.pandanav.learning.domain.enums.TaskStatus;

import java.time.OffsetDateTime;

public class PracticeFeedbackReport {

    private Long id;
    private Long quizId;
    private Long sessionId;
    private Long taskId;
    private Long userId;
    private String diagnosisSummary;
    private String strengthsJson;
    private String weaknessesJson;
    private String reviewFocusJson;
    private String nextRoundAdvice;
    private String recommendedAction;
    private String selectedAction;
    private String source;
    private String promptVersion;
    private TaskStatus reportStatus;
    private OffsetDateTime reportStartedAt;
    private OffsetDateTime reportFinishedAt;
    private OffsetDateTime actionSelectedAt;
    private String traceId;
    private Integer tokenInput;
    private Integer tokenOutput;
    private Integer latencyMs;
    private String lastErrorCode;
    private OffsetDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getQuizId() {
        return quizId;
    }

    public void setQuizId(Long quizId) {
        this.quizId = quizId;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getDiagnosisSummary() {
        return diagnosisSummary;
    }

    public void setDiagnosisSummary(String diagnosisSummary) {
        this.diagnosisSummary = diagnosisSummary;
    }

    public String getStrengthsJson() {
        return strengthsJson;
    }

    public void setStrengthsJson(String strengthsJson) {
        this.strengthsJson = strengthsJson;
    }

    public String getWeaknessesJson() {
        return weaknessesJson;
    }

    public void setWeaknessesJson(String weaknessesJson) {
        this.weaknessesJson = weaknessesJson;
    }

    public String getReviewFocusJson() {
        return reviewFocusJson;
    }

    public void setReviewFocusJson(String reviewFocusJson) {
        this.reviewFocusJson = reviewFocusJson;
    }

    public String getNextRoundAdvice() {
        return nextRoundAdvice;
    }

    public void setNextRoundAdvice(String nextRoundAdvice) {
        this.nextRoundAdvice = nextRoundAdvice;
    }

    public String getRecommendedAction() {
        return recommendedAction;
    }

    public void setRecommendedAction(String recommendedAction) {
        this.recommendedAction = recommendedAction;
    }

    public String getSelectedAction() {
        return selectedAction;
    }

    public void setSelectedAction(String selectedAction) {
        this.selectedAction = selectedAction;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getPromptVersion() {
        return promptVersion;
    }

    public void setPromptVersion(String promptVersion) {
        this.promptVersion = promptVersion;
    }

    public TaskStatus getReportStatus() {
        return reportStatus;
    }

    public void setReportStatus(TaskStatus reportStatus) {
        this.reportStatus = reportStatus;
    }

    public OffsetDateTime getReportStartedAt() {
        return reportStartedAt;
    }

    public void setReportStartedAt(OffsetDateTime reportStartedAt) {
        this.reportStartedAt = reportStartedAt;
    }

    public OffsetDateTime getReportFinishedAt() {
        return reportFinishedAt;
    }

    public void setReportFinishedAt(OffsetDateTime reportFinishedAt) {
        this.reportFinishedAt = reportFinishedAt;
    }

    public OffsetDateTime getActionSelectedAt() {
        return actionSelectedAt;
    }

    public void setActionSelectedAt(OffsetDateTime actionSelectedAt) {
        this.actionSelectedAt = actionSelectedAt;
    }

    public String getTraceId() {
        return traceId;
    }

    public void setTraceId(String traceId) {
        this.traceId = traceId;
    }

    public Integer getTokenInput() {
        return tokenInput;
    }

    public void setTokenInput(Integer tokenInput) {
        this.tokenInput = tokenInput;
    }

    public Integer getTokenOutput() {
        return tokenOutput;
    }

    public void setTokenOutput(Integer tokenOutput) {
        this.tokenOutput = tokenOutput;
    }

    public Integer getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(Integer latencyMs) {
        this.latencyMs = latencyMs;
    }

    public String getLastErrorCode() {
        return lastErrorCode;
    }

    public void setLastErrorCode(String lastErrorCode) {
        this.lastErrorCode = lastErrorCode;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
