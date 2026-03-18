package navigator.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("task_message")
public class TaskMessageEntity {
    private Long id;
    private String sessionKey;
    private String taskCode;
    private String role;
    private String content;
    private String detectedAction;
    private String stateBefore;
    private String stateAfter;
    private String fallbackMode;
    private LocalDateTime createdAt;
}

