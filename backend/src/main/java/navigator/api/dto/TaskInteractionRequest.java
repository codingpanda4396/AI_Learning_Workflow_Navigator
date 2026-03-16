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
public class TaskInteractionRequest {
    private String sessionId;
    private String interactionType;
    private Integer interactionCountDelta;
    private Boolean userSummarySubmitted;
    private List<String> behaviorSignals;
    private List<String> detectedIssueTags;
    private String contentSummary;
}
