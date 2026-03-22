package navigator.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import navigator.domain.enums.ScaffoldPromptIntent;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScaffoldPrompt {
    private String promptId;
    private String prompt;
    private ScaffoldPromptIntent intent;
    private boolean required;
}
