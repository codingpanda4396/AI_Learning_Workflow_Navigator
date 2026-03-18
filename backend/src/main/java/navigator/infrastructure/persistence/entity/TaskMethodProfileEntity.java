package navigator.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("task_method_profile")
public class TaskMethodProfileEntity {
    private Long id;
    private String sessionKey;
    private String taskCode;
    private String profileJson;
    private LocalDateTime createdAt;
}

