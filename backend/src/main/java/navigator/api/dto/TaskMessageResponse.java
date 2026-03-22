package navigator.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import navigator.domain.model.TaskExecutionEvidenceDelta;

import java.util.List;

/**
 * POST /api/tasks/{taskId}/messages 响应。
 * 合同冻结：assistantReply, taskState, detectedAction 为稳定字段。
 * nextSuggestedPrompts 与 scaffold.recommendedFollowupTemplates 语义对应。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskMessageResponse {
    private String assistantReply;
    private String detectedAction;
    private String taskState;
    private List<String> nextSuggestedPrompts;
    private String fallbackMode;

    private String guidanceIntent;
    private String guidancePhase;
    private TaskExecutionEvidenceDelta evidenceDelta;
    private Boolean whetherCanComplete;
    private List<RecommendedUserActionItem> recommendedUserActions;
}
