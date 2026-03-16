package navigator.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import navigator.domain.enums.NextActionType;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NextActionDecision {
    private NextActionType actionType;
    private String reason;
    private String nextEntryPoint;
    private List<String> adjustmentSignals;
    private boolean requiresReplan;
}
