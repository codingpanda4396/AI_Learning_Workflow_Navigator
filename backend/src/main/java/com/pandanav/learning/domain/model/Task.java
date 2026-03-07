package com.pandanav.learning.domain.model;

import com.pandanav.learning.domain.enums.Stage;
import com.pandanav.learning.domain.enums.TaskStatus;

import java.time.OffsetDateTime;

public class Task {

    private Long id;
    private Long sessionId;
    private Stage stage;
    private Long nodeId;
    private String objective;
    private TaskStatus status;
    private String outputJson;
    private String failureReason;
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

    public Stage getStage() {
        return stage;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public Long getNodeId() {
        return nodeId;
    }

    public void setNodeId(Long nodeId) {
        this.nodeId = nodeId;
    }

    public String getObjective() {
        return objective;
    }

    public void setObjective(String objective) {
        this.objective = objective;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public String getOutputJson() {
        return outputJson;
    }

    public void setOutputJson(String outputJson) {
        this.outputJson = outputJson;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public boolean canRun() {
        return status == TaskStatus.PENDING || status == TaskStatus.FAILED;
    }

    public boolean canSubmit() {
        return stage == Stage.TRAINING && (status == TaskStatus.SUCCEEDED || status == TaskStatus.PENDING);
    }

    public void markRunning() {
        if (status != TaskStatus.PENDING && status != TaskStatus.FAILED) {
            throw new IllegalStateException("Task is not in a runnable state.");
        }
        this.status = TaskStatus.RUNNING;
    }

    public void markSucceeded(String outputJson) {
        this.status = TaskStatus.SUCCEEDED;
        this.outputJson = outputJson;
        this.failureReason = null;
    }

    public void markFailed(String reason) {
        this.status = TaskStatus.FAILED;
        this.failureReason = reason;
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


