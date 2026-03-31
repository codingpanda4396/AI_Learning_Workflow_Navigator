package navigator.api.dto.scaffold;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 用户产出形态：前端按 mode 渲染字段或段落。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExpressionSchemaPayload {
    /** form | paragraph | compare | causal_chain | error_reflection | one_sentence_rule */
    private String mode;
    /** 与 promptScaffold block id 对齐的输入槽（可多字段） */
    @Builder.Default
    private List<String> fieldIds = new ArrayList<>();
    private Integer minChars;
    private Integer maxChars;
}
