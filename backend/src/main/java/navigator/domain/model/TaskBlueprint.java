package navigator.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import navigator.domain.enums.TaskType;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskBlueprint {
    private String taskId;
    private String title;
    private TaskType taskType;
    private String goal;
    /** 学习方法引导（如何借助 LLM） */
    private String taskMethod;
    /** 推荐向 LLM 提问的模板，优先于 promptScaffold */
    private String recommendedPromptTemplate;
    private Integer estimatedMinutes;
    /** @deprecated 使用 recommendedPromptTemplate */
    private String promptScaffold;
    private List<String> completionCriteria;
    private List<String> evidenceToCollect;
    /** 自评问题列表 */
    private List<String> selfEvaluationQuestions;
    private String fallbackAction;
}
