package navigator.infrastructure.persistence.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SessionTaskEntity {
    private Long id;
    private Long sessionId;
    private Long planId;
    private String stageCode;
    private String taskCode;
    private String taskType;
    private Integer orderIndex;
    private String title;
    private String objective;
    private String taskSnapshotJson;
    private String completionCriteriaJson;
    private Integer estimatedMinutes;
    private String status;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime lastInteractedAt;
    private LocalDateTime createdAt;
}

