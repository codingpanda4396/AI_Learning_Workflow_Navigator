package navigator.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanStage {
    private String stageCode;
    private String title;
    private String objective;
    private Integer estimatedMinutes;
}
