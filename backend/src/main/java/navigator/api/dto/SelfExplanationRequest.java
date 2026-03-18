package navigator.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SelfExplanationRequest {
    @NotBlank
    private String sessionId;
    @NotBlank
    private String content;
}
