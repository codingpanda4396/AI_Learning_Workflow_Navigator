package navigator.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("session_task")
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

