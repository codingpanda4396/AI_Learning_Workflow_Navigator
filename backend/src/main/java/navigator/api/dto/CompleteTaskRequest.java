package navigator.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import navigator.domain.enums.SelfRatedConfidence;
import navigator.domain.enums.TaskCompletionStatus;
import navigator.domain.model.TaskExecutionEvidenceSnapshot;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompleteTaskRequest {
    private String sessionId;
    private TaskCompletionStatus completionStatus;
    private Integer durationMinutes;
    private Integer interactionCount;
    private Boolean userSummarySubmitted;
    private String microPracticeResult;
    private List<String> detectedIssueTags;
    private List<String> behaviorSignals;
    private String learnerReflection;

    /** Sprint 4 收束字段（写入完成记录；完成接口不再强制校验） */
    private String summaryText;
    private List<String> learnedFrameworkPoints;
    private List<String> unresolvedQuestions;
    private String nextPracticeIntent;
    private SelfRatedConfidence selfRatedConfidence;
    private String closurePayloadVersion;

    /** 服务端写入，覆盖客户端伪造 */
    private TaskExecutionEvidenceSnapshot evidenceSnapshot;
}
