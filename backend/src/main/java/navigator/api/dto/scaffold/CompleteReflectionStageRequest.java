package navigator.api.dto.scaffold;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 与前端「反思收敛」单页（策略勾选 + 一句话）对齐，一次性收口 REFLECTION 并置任务执行为 PASS。
 */
@Data
public class CompleteReflectionStageRequest {
    @NotBlank
    private String sessionId;

    /** 学习者一句话反思（可与策略共同构成最小完成条件） */
    private String reflectionText;

    /** 勾选策略的展示文案，顺序保留 */
    private List<String> strategyLabels = new ArrayList<>();
}
