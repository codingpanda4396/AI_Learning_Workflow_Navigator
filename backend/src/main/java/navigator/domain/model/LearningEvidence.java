package navigator.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningEvidence {
    private boolean attemptedExplain;
    private boolean containsKeyConcept;
    private boolean hasMisconception;
    private boolean askingForAnswer;
    private boolean confusedSignal;
    private int interactionDepth;
}
