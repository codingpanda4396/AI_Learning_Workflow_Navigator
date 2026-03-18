package navigator.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("task_execution_runtime")
public class TaskExecutionRuntimeEntity {
    private Long id;
    private String sessionKey;
    private Long sessionId;
    private Long taskId;
    private String taskCode;
    private String scaffoldId;
    private String scaffoldJson;
    private String currentState;
    private Integer exploreTurnCount;
    private String checkpointQuestion;
    private String selfExplanationEvaluation;
    private String actionHistoryJson;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

