package navigator.api.dto.scaffold;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 驱动「脚手架工作台」的统一载荷：前端只渲染槽位，知识软内容尽量来自 LLM + 本对象。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StageScaffoldWorkbenchPayload {
    private String stageKey;
    /** 认知动作短语，如「先搭骨架」 */
    private String cognitiveAction;
    private String stageGoal;
    private String currentTaskTitle;
    private String currentTaskInstruction;
    private String deliverable;
    @Builder.Default
    private List<String> completionCriteria = new ArrayList<>();
    private PromptScaffold promptScaffold;
    private ExpressionSchemaPayload expressionSchema;
    @Builder.Default
    private List<String> starterPrompts = new ArrayList<>();
    @Builder.Default
    private List<String> hintPrompts = new ArrayList<>();
    private WorkbenchFeedbackSchemaPayload feedbackSchema;
    private String llmGeneratedGuide;
    private String llmGeneratedMicroHint;
    private String llmGeneratedExampleBoundary;
    private String submitConstraint;
    /** STRUCTURE | UNDERSTANDING | TRAINING | REFLECTION — 驱动前端换台 */
    private String emphasisMode;
}
