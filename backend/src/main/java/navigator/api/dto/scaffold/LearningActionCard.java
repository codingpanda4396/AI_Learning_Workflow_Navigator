package navigator.api.dto.scaffold;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningActionCard {
    private String actionId;
    private String title;
    private String goal;
    private String instructions;
    private String userOutputLabel;
    private List<String> allowedPrompts;
    private List<String> forbiddenPrompts;
    private List<String> passCriteria;
    private String exampleOutput;
    private String nextActionHint;
}
