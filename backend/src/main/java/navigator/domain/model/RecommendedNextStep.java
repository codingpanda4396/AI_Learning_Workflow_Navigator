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
public class RecommendedNextStep {
    private NextActionType actionType;
    private String title;
    private String reason;
    private String actionLabel;
    private String nextEntryPoint;
    private List<String> signals;
    private boolean requiresReplan;
}
