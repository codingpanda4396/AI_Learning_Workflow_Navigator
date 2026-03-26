package navigator.api.dto.scaffold;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * STRUCTURE 阶段「知识骨架」展示块（与 LLM JSON 或模板兜底对齐）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StructureSkeletonBlock {
    private String module;
    @Builder.Default
    private List<String> prerequisites = new ArrayList<>();
    @Builder.Default
    private List<String> connections = new ArrayList<>();
    @Builder.Default
    private List<String> deferTopics = new ArrayList<>();
}
