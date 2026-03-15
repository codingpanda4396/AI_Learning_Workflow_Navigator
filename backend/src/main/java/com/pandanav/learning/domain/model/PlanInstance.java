package com.pandanav.learning.domain.model;

import com.pandanav.learning.domain.enums.PlanInstanceStatus;

import java.time.OffsetDateTime;

public class PlanInstance {

    private Long id;
    private Long sessionId;
    private Long sourcePlanId;
    private PlanInstanceStatus status;
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

    public Long getSourcePlanId() {
        return sourcePlanId;
    }

    public void setSourcePlanId(Long sourcePlanId) {
        this.sourcePlanId = sourcePlanId;
    }

    public PlanInstanceStatus getStatus() {
        return status;
    }

    public void setStatus(PlanInstanceStatus status) {
        this.status = status;
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
