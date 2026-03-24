package navigator.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecentLearningEntryData {
    private String goalId;
    private String diagnosisId;
    private String planId;
    private String sessionId;
    private String currentTaskId;
    private String sessionStatus;
}
