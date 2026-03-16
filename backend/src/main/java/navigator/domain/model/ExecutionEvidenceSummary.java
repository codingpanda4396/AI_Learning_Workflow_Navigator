package navigator.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecutionEvidenceSummary {
    private Integer totalTasks;
    private Integer completedTasks;
    private Integer totalDurationMinutes;
    private Integer totalInteractionCount;
    private boolean summarySubmitted;
    private List<String> aggregatedIssueTags;
    private List<String> keyBehaviorSignals;
    private List<String> evidenceHighlights;
}
