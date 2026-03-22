package navigator.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 认知单元：连接「任务」与「脚手架提问」的中间层，表达本微步的认知目标与完成信号。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CognitiveUnit {
    private String unitId;
    private int order;
    /** 步骤条短标签 */
    private String label;
    private String learningObjective;
    private String targetOutcome;
    private String failureSignal;
    private List<String> actionBullets;
    private List<ScaffoldPrompt> prompts;
}
