package navigator.infrastructure.persistence.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class TaskInteractionEntity {
    private Long id;
    private Long sessionId;
    private Long taskId;
    private String interactionType;
    private String userInput;
    private String assistantOutputSummary;
    private String extractedSignalsJson;
    private LocalDateTime createdAt;
}

