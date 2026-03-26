package navigator.api.dto.scaffold;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompleteStructureStageResult {
    private boolean structureStageComplete;
    private String nextStageKey;
    private String nextActionId;
}
