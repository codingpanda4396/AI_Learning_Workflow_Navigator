package navigator.api.dto.scaffold;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ActionRuntime {
    private String sessionId;
    private String stageKey;
    private String actionId;
    private String userInput;
    private String validationStatus;
    private String tutorFeedback;
    private int retryCount;
    private boolean completed;
    private int attemptNo;
    private String runtimeStatus;
    private Boolean canProceed;
}
