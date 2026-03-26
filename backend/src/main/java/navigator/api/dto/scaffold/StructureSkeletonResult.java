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
public class StructureSkeletonResult {
    private StructureSkeletonBlock skeleton;
    /** 成功生成骨架的次数（含首次点卡） */
    private int structureGenerationCount;
    private int structureLightInteractionCount;
    @Builder.Default
    private List<String> structureExploredPromptKeys = new ArrayList<>();
    private boolean canCompleteStructure;
    private String lastPromptKey;
}
