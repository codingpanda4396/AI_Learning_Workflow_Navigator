package navigator.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionFlowStateData {
    private String sessionId;
    private String sessionStatus;
    private String currentPhase;
    private String currentRoute;
    private String currentTaskId;
    private boolean reportReady;
    private int completedTaskCount;
    private int totalTaskCount;
}
