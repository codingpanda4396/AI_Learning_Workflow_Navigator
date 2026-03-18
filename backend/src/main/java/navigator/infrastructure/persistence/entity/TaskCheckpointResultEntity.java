package navigator.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("task_checkpoint_result")
public class TaskCheckpointResultEntity {
    private Long id;
    private String sessionKey;
    private String taskCode;
    private String question;
    private String answer;
    private String result;
    private String reason;
    private String suggestedRemedialAction;
    private LocalDateTime createdAt;
}

