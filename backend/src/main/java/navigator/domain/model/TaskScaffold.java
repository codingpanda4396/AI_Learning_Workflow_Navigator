package navigator.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskScaffold {
    private String scaffoldId;
    private String taskId;
    private String sessionId;
    private String taskType;
    private String learningObjective;
    private String whyThisTask;
    private List<String> recommendedAskTemplates;
    private List<String> recommendedFollowupTemplates;
    private List<String> selfCheckTemplates;
    private List<String> fallbackHints;
    private List<String> completionSignals;
    private List<String> antiPatterns;
    private String currentExecutionState;
    private Integer suggestedExploreTurns;
    private Integer suggestedCheckpointCount;
    private LocalDateTime createdAt;
}
