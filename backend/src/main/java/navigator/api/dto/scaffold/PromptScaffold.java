package navigator.api.dto.scaffold;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptScaffold {
    @Builder.Default
    private List<PromptScaffoldBlock> blocks = new ArrayList<>();
}
