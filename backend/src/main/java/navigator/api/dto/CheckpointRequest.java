package navigator.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CheckpointRequest {
    @NotBlank
    private String sessionId;
    @NotBlank
    private String answer;
}
