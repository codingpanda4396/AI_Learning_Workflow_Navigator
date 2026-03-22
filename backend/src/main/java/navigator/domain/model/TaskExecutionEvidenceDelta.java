package navigator.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskExecutionEvidenceDelta {
    @Builder.Default
    private int totalTurnsDelta = 0;
    @Builder.Default
    private int userInitiatedQuestionTurnsDelta = 0;
    @Builder.Default
    private int assistantHintTurnsDelta = 0;
    @Builder.Default
    private int assistantRedirectTurnsDelta = 0;
    @Builder.Default
    private int directAnswerSeekCountDelta = 0;
    @Builder.Default
    private int vagueUserReplyCountDelta = 0;
    @Builder.Default
    private int confusionSignalsCountDelta = 0;
}
