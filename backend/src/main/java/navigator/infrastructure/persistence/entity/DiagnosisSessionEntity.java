package navigator.infrastructure.persistence.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DiagnosisSessionEntity {
    private Long id;
    private Long sessionId;
    private Long goalId;
    private String status;
    private String generationMode;
    /**
     * 序列化后的诊断问题列表 JSON
     */
    private String questionsJson;
    private LocalDateTime submittedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
}

