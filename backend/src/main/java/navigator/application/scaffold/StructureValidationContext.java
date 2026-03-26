package navigator.application.scaffold;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StructureValidationContext {
    private String packId;
    private String stageKey;
    private String actionId;
    private String userInput;
}
