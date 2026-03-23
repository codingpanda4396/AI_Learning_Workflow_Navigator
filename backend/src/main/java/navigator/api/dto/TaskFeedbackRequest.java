package navigator.api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TaskFeedbackRequest {
    @NotBlank
    private String answer;
}
