package navigator.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import navigator.domain.model.TaskBlueprint;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CurrentTaskData {
    private String sessionId;
    private CurrentTaskItem currentTask;
    private ProgressItem progress;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CurrentTaskItem {
        private String taskId;
        private String title;
        private String taskType;
        private String goal;
        private String whyThisTask;
        private String taskMethod;
        private String recommendedPromptTemplate;
        private Integer estimatedMinutes;
        private String promptScaffold;
        private java.util.List<String> completionCriteria;
        private java.util.List<String> selfEvaluationQuestions;
        private String fallbackAction;
        /** 来自 ExecutableTaskSpec.evaluationRubric 的摘要 */
        private String evaluationRubricSummary;
        /** 来自 ExecutableTaskSpec.scaffoldPolicy 的摘要 */
        private String scaffoldPolicySummary;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProgressItem {
        private int currentIndex;
        private int totalTasks;
    }
}
