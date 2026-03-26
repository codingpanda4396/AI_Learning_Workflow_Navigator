package navigator.api.dto.scaffold;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompleteStructureStageRequest {
    @NotBlank
    private String sessionId;
    /** 可选：一句话位置感受 */
    private String optionalOneLiner;
}
