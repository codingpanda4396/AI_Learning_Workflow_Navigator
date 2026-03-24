package navigator.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("diagnosis_session")
public class DiagnosisSessionEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
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
