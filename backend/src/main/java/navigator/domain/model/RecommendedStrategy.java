package navigator.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import navigator.domain.enums.RecommendedStrategyCode;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendedStrategy {
    private RecommendedStrategyCode code;
    private String label;
    private String reason;
}
