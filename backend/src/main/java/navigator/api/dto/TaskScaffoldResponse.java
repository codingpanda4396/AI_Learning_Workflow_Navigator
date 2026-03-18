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
public class TaskScaffoldResponse {
    private String taskId;
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
}
