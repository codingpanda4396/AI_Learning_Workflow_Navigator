package navigator.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("learning_plan")
public class LearningPlanEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sessionId;
    private Long goalId;
    private Long diagnosisSessionId;
    private String status;
    private String strategyCode;
    private String recommendedEntryJson;
    private String planSnapshotJson;
    private String successCriteriaJson;
    private String risksJson;
    private String keyEvidenceJson;
    private LocalDateTime committedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

