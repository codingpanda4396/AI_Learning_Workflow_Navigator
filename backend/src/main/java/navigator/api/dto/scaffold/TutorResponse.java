package navigator.api.dto.scaffold;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TutorResponse {
    private String feedbackType;
    private String content;
    private String nextPrompt;
    private boolean canProceed;
}
