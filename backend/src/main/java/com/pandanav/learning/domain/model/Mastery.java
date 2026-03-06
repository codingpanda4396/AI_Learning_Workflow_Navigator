package com.pandanav.learning.domain.model;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public class Mastery {

    private String userId;
    private Long nodeId;
    private String nodeName;
    private BigDecimal masteryValue;
    private OffsetDateTime updatedAt;

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
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

    public BigDecimal getMasteryValue() {
        return masteryValue;
    }

    public void setMasteryValue(BigDecimal masteryValue) {
        this.masteryValue = masteryValue;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
