package navigator.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("learning_session")
public class LearningSessionEntity {
    private Long id;
    private Long goalId;
    private Long diagnosisSessionId;
    private Long planId;
    private String status;
    private Long currentTaskId;
    private Integer totalTaskCount;
    private Integer completedTaskCount;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

