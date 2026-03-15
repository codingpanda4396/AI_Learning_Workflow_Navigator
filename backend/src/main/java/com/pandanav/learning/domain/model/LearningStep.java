package com.pandanav.learning.domain.model;

import com.pandanav.learning.domain.enums.LearningStepStatus;
import com.pandanav.learning.domain.enums.Stage;

import java.time.OffsetDateTime;

public class LearningStep {

    private Long id;
    private Long taskId;
    private Stage stage;
    private String type;
    private Integer stepOrder;
    private LearningStepStatus status;
    private String objective;
    private CompletionRule completionRule;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public Stage getStage() {
        return stage;
    }

    public void setStage(Stage stage) {
        this.stage = stage;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getStepOrder() {
        return stepOrder;
    }

    public void setStepOrder(Integer stepOrder) {
        this.stepOrder = stepOrder;
    }

    public LearningStepStatus getStatus() {
        return status;
    }

    public void setStatus(LearningStepStatus status) {
        this.status = status;
    }

    public String getObjective() {
        return objective;
    }

    public void setObjective(String objective) {
        this.objective = objective;
    }

    public CompletionRule getCompletionRule() {
        return completionRule;
    }

    public void setCompletionRule(CompletionRule completionRule) {
        this.completionRule = completionRule;
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

    public void activate() {
        transitionTo(LearningStepStatus.ACTIVE);
    }

    public void markDone() {
        transitionTo(LearningStepStatus.DONE);
    }

    public void markFailed() {
        transitionTo(LearningStepStatus.FAILED);
    }

    public void markSkipped() {
        transitionTo(LearningStepStatus.SKIPPED);
    }

    private void transitionTo(LearningStepStatus target) {
        if (status == null) {
            throw new IllegalStateException("LearningStep status is required.");
        }
        if (status == LearningStepStatus.TODO && target == LearningStepStatus.ACTIVE) {
            status = target;
            return;
        }
        if (status == LearningStepStatus.ACTIVE && target.isTerminal()) {
            status = target;
            return;
        }
        throw new IllegalStateException("Illegal learning step transition: " + status + " -> " + target);
    }
}

