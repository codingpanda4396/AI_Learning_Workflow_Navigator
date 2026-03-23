package navigator.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AiTutorPrefetchRequest {
    @NotBlank
    private String step;
    @NotBlank
    private String knowledgePoint;
}
