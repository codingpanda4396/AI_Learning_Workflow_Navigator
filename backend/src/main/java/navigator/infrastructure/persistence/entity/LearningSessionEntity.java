package navigator.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("learning_session")
public class LearningSessionEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
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
