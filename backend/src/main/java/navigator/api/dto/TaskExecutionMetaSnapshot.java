package navigator.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 任务执行页元信息（不含阶段正文与 LLM）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskExecutionMetaSnapshot {
    /** 通常为 packId，缺省时为 knowledgeKey */
    private String knowledge;
    /** 脚手架引擎当前阶段；未启用引擎时为 null */
    private String currentStage;
    /** 四阶段是否已完成 */
    private Map<String, Boolean> progressMap;
}
