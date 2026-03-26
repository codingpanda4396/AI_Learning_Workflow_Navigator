package navigator.api.dto.scaffold;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainingDetectedProblem {
    private String problemText;
    /** MECHANISM_ERROR | CAUSAL_GAP | VAGUE_EXPRESSION | MISSING_STEP */
    private String errorType;
    private String evidence;
    private String fixHint;
}
