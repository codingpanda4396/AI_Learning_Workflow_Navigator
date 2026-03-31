package navigator.api.dto.scaffold;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 单块脚手架槽位：结构由规则/定义确定，文案可由 LLM 软生成。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromptScaffoldBlock {
    private String id;
    private String title;
    private String intent;
    private String prompt;
    private String placeholder;
    private String constraint;
    private Integer maxLength;
    private Integer sentenceLimit;
    private Boolean required;
    /** paragraph | short | chips | readonly */
    private String kind;
}
