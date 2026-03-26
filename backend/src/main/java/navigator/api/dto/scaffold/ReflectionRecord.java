package navigator.api.dto.scaffold;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 反思沉淀：错误 → 根因 → 规律 → 能力 → 下一步策略。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReflectionRecord {
    private String errorPattern;
    private String rootCause;
    private String decisionRule;
    private String capabilityName;
    private String futureStrategy;
}
