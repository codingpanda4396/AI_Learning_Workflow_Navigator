package navigator.infrastructure.persistence.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class LearnerProfileSnapshotEntity {
    private Long id;
    private Long diagnosisSessionId;
    private Long sessionId;
    /**
     * 序列化后的 LearnerProfileSnapshot JSON
     */
    private String profileJson;
    /**
     * 规划模式（当前暂未使用，可为 null）
     */
    private String planningMode;
    /**
     * 推荐的入口策略 / 起点
     */
    private String entryStrategy;
    /**
     * 推荐的粒度（如：topic / subtopic / project）
     */
    private String entryGranularity;
    /**
     * 推荐的反馈频率 / 模式
     */
    private String feedbackMode;
    /**
     * 风险标签 JSON
     */
    private String riskTagsJson;
    /**
     * 关键诊断证据 JSON
     */
    private String keyEvidenceJson;
    private LocalDateTime createdAt;
}

