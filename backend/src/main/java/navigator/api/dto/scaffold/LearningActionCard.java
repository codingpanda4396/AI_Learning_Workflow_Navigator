package navigator.api.dto.scaffold;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningActionCard {
    private String actionId;
    private String title;
    private String goal;
    /** 当前卡片要求用户完成的唯一动作 */
    private String singleAction;
    private String instructions;
    /** 输入框上方的固定引导语 */
    private String systemPrompt;
    /** 当前阶段系统反馈的人设/角色 */
    private String llmRole;
    private String userOutputLabel;
    private List<String> allowedPrompts;
    private List<String> forbiddenPrompts;
    private List<String> forbiddenActions;
    private List<String> passCriteria;
    private List<String> completionCriteria;
    private String exampleOutput;
    private String nextActionHint;
}
