package navigator.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckpointResponse {
    private String result;
    private String reason;
    private String suggestedRemedialAction;
    private String taskState;
}
