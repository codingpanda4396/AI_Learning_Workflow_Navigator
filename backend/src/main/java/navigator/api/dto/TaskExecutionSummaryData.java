package navigator.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import navigator.domain.model.TaskExecutionEvidenceSnapshot;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskExecutionSummaryData {
    private String sessionId;
    private String taskId;
    private String taskExecutionState;
    private String guidancePhase;
    private Map<String, Boolean> phaseProgress;
    private TaskExecutionEvidenceSnapshot evidenceSnapshot;
    private List<CompletionRequirementItem> missingCompletionRequirements;
}
