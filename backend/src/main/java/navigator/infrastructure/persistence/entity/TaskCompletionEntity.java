package navigator.infrastructure.persistence.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TaskCompletionEntity {
    private Long id;
    private Long sessionId;
    private Long taskId;
    private String completionInputJson;
    private String completionStatus;
    private String qualityLevel;
    private String detectedGapTagsJson;
    private String riskTagsJson;
    private String nextActionHintsJson;
    private LocalDateTime createdAt;
}

