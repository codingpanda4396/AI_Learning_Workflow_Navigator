package navigator.api.dto.scaffold;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StructureSkeletonRequest {
    @NotBlank
    private String sessionId;
    /**
     * 四张脚手架卡之一，见 {@link navigator.application.scaffold.DfsBfsStructureScaffoldDefinition}
     */
    @NotBlank
    private String promptKey;
    /**
     * 可选：CLARIFY=再解释一次，ADJACENT=相邻概念关系；首次点卡不传或空。
     */
    private String followUpKind;
}
