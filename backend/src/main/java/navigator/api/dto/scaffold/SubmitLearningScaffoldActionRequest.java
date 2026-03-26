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
public class SubmitLearningScaffoldActionRequest {
    @NotBlank
    private String sessionId;
    @NotBlank
    private String stageKey;
    @NotBlank
    private String actionId;
    private String userInput;
}
