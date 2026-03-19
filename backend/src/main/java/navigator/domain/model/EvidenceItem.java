package navigator.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 结构化证据条目：用于把执行期输入/回合/检查点沉淀为可追溯证据。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvidenceItem {
    /** 证据类型：NOTE/CODE/SUMMARY/CHECKPOINT/... */
    private String type;
    /** 证据摘要（短文本） */
    private String summary;
    /** 证据来源：USER_MESSAGE/ASSISTANT_MESSAGE/CHECKPOINT/SYSTEM/... */
    private String source;
    /** 附加结构化字段（轻量扩展点） */
    private Map<String, Object> attrs;
    private LocalDateTime createdAt;
}

