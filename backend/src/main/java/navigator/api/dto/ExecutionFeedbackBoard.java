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
public class ExecutionFeedbackBoard {
    private String correct;
    private String missing;
    private String confused;
    private String nextFix;
    private List<ExecutionFeedbackActionItem> actions;
}
