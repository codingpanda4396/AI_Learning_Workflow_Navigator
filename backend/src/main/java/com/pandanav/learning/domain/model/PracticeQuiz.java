package com.pandanav.learning.domain.model;

import com.pandanav.learning.domain.enums.TaskStatus;
import com.pandanav.learning.domain.enums.PracticeQuizStatus;

import java.time.OffsetDateTime;

public class PracticeQuiz {

    private Long id;
    private Long sessionId;
    private Long taskId;
    private Long userId;
    private Long nodeId;
    private PracticeQuizStatus status;
    private Integer questionCount;
    private Integer answeredCount;
    private String generationSource;
    private String promptVersion;
    private String failureReason;
    private TaskStatus generationStatus;
    private OffsetDateTime generationStartedAt;
    private OffsetDateTime generationFinishedAt;
    private String traceId;
    private Integer tokenInput;
    private Integer tokenOutput;
    private Integer latencyMs;
    private String lastErrorCode;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Long getNodeId() {
        return nodeId;
    }

    public void setNodeId(Long nodeId) {
        this.nodeId = nodeId;
    }

    public PracticeQuizStatus getStatus() {
        return status;
    }

    public void setStatus(PracticeQuizStatus status) {
        this.status = status;
    }

    public Integer getQuestionCount() {
        return questionCount;
    }

    public void setQuestionCount(Integer questionCount) {
        this.questionCount = questionCount;
    }

    public Integer getAnsweredCount() {
        return answeredCount;
    }

    public void setAnsweredCount(Integer answeredCount) {
        this.answeredCount = answeredCount;
    }

    public String getGenerationSource() {
        return generationSource;
    }

    public void setGenerationSource(String generationSource) {
        this.generationSource = generationSource;
    }

    public String getPromptVersion() {
        return promptVersion;
    }

    public void setPromptVersion(String promptVersion) {
        this.promptVersion = promptVersion;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public TaskStatus getGenerationStatus() {
        return generationStatus;
    }

    public void setGenerationStatus(TaskStatus generationStatus) {
        this.generationStatus = generationStatus;
    }

    public OffsetDateTime getGenerationStartedAt() {
        return generationStartedAt;
    }

    public void setGenerationStartedAt(OffsetDateTime generationStartedAt) {
        this.generationStartedAt = generationStartedAt;
    }

    public OffsetDateTime getGenerationFinishedAt() {
        return generationFinishedAt;
    }

    public void setGenerationFinishedAt(OffsetDateTime generationFinishedAt) {
        this.generationFinishedAt = generationFinishedAt;
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

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
