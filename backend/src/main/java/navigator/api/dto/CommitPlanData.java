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
public class CommitPlanData {
    private String sessionId;
    private String planId;
    private List<String> taskSequence;
    private String currentTaskId;
    private String status;
}
