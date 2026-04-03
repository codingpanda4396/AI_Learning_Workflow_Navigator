package navigator.api.dto.scaffold;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompleteReflectionStageResult {
    private boolean reflectionStageComplete;
    private String completedStageKey;
    /** 反思阶段为最后一阶，通常无「下一阶段」 */
    private String nextStageKey;
    private String nextActionId;
}
