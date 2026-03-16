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
public class TaskExecutionRecord {
    private String taskId;
    private String taskType;
    private String completionStatus;
    private Integer durationMinutes;
    private Integer interactionCount;
    private boolean userSummarySubmitted;
    private String microPracticeResult;
    private List<String> detectedIssueTags;
    private List<String> behaviorSignals;
    private String learnerReflection;
}
