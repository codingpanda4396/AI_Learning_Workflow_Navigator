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
public class SelfExplanationResponse {
    private String evaluation;
    private List<String> missingPoints;
    private String nextAction;
    private String taskState;
    /** 进入 CHECK 时的微检查题干 */
    private String checkpointQuestion;
    private ExecutionFeedbackBoard feedbackBoard;
}
