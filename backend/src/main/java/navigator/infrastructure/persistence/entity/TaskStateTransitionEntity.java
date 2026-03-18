package navigator.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("task_state_transition")
public class TaskStateTransitionEntity {
    private Long id;
    private String sessionKey;
    private String taskCode;
    private String fromState;
    private String toState;
    private String reason;
    private LocalDateTime createdAt;
}

