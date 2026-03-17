package navigator.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("task_interaction")
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

