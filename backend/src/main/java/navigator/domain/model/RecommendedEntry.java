package navigator.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendedEntry {
    private String conceptId;
    private String title;
    private Integer estimatedMinutes;
    private String reason;
}
