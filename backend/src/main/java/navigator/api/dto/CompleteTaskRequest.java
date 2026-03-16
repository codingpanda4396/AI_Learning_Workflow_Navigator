package navigator.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import navigator.domain.enums.TaskCompletionStatus;

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
}
