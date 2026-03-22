package navigator.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CurrentTaskGuidanceData {
    private String sessionId;
    private String taskId;
    private String taskExecutionState;
    private String guidancePhase;
    private CurrentGuidanceBlock currentGuidance;
    private List<RecommendedUserActionItem> recommendedUserActions;
    private List<CompletionRequirementItem> completionRequirements;
    private String policyVersion;
}
