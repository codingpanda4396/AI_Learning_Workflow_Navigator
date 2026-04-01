package navigator.api.dto.scaffold;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CompleteConversationStageRequest {
    @NotBlank
    private String sessionId;

    @NotBlank
    private String stageKey;

    private String finalDraft;
}
