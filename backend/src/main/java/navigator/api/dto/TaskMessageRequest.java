package navigator.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TaskMessageRequest {
    @NotBlank
    private String sessionId;
    /** USER */
    private String role;
    @NotBlank
    private String content;
}
