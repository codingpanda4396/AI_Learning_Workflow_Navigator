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
public class TaskMessageResponse {
    private String assistantReply;
    private String detectedAction;
    private String taskState;
    private List<String> nextSuggestedPrompts;
    private String fallbackMode;
}
