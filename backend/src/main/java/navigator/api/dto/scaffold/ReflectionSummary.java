package navigator.api.dto.scaffold;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 任务级可展示的反思沉淀（供 EXPLORE 驾驶舱摘要）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReflectionSummary {
    private ReflectionRecord record;
    private ReflectionInsight insight;
    /** 一句系统侧总览，便于答辩展示 */
    private String systemObservation;
}
