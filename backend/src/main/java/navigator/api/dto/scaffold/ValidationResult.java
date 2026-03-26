package navigator.api.dto.scaffold;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationResult {
    private boolean passed;
    private String errorType;
    private String message;
    private List<String> suggestions;
    /** 本轮表达已覆盖的要点（供前端与 Tutor 展示） */
    private List<String> matchedAspects;
    /** 仍缺失的机制槽位或要点 */
    private List<String> missingAspects;
}
