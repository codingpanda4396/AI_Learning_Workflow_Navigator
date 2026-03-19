package navigator.application.planning;

import navigator.domain.model.PlanStage;
import navigator.domain.model.TaskBlueprint;
import navigator.domain.model.TimeBudgetConstraint;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 按时间预算约束裁剪/压缩任务列表。
 */
@Component
public class TimeBudgetEnforcer {

    public PlanTemplateFactory.StagesAndTasks applyConstraint(
            PlanTemplateFactory.StagesAndTasks original,
            TimeBudgetConstraint constraint) {
        if (constraint == null || constraint.getTotalMinutesCap() < 0) {
            return original;
        }
        List<TaskBlueprint> tasks = new ArrayList<>(original.tasks);
        int cap = constraint.getTotalMinutesCap();
        int maxTasks = constraint.getMaxTasks();

        int total = tasks.stream().mapToInt(t -> t.getEstimatedMinutes() != null ? t.getEstimatedMinutes() : 10).sum();
        if (total <= cap && tasks.size() <= maxTasks) {
            return original;
        }

        List<TaskBlueprint> trimmed = tasks.size() > maxTasks
                ? new ArrayList<>(tasks.subList(0, maxTasks))
                : new ArrayList<>(tasks);

        int trimmedTotal = trimmed.stream().mapToInt(t -> t.getEstimatedMinutes() != null ? t.getEstimatedMinutes() : 10).sum();
        if (trimmedTotal > cap && !trimmed.isEmpty()) {
            int perTask = cap / trimmed.size();
            List<TaskBlueprint> rescaled = new ArrayList<>();
            for (TaskBlueprint t : trimmed) {
                rescaled.add(TaskBlueprint.builder()
                        .taskId(t.getTaskId())
                        .title(t.getTitle())
                        .taskType(t.getTaskType())
                        .goal(t.getGoal())
                        .taskMethod(t.getTaskMethod())
                        .recommendedPromptTemplate(t.getRecommendedPromptTemplate())
                        .promptScaffold(t.getPromptScaffold())
                        .completionCriteria(t.getCompletionCriteria())
                        .evidenceToCollect(t.getEvidenceToCollect())
                        .selfEvaluationQuestions(t.getSelfEvaluationQuestions())
                        .fallbackAction(t.getFallbackAction())
                        .estimatedMinutes(perTask)
                        .build());
            }
            trimmed = rescaled;
        }

        List<PlanStage> stages = rescaleStages(original.stages, trimmed.size());
        return new PlanTemplateFactory.StagesAndTasks(stages, trimmed);
    }

    private List<PlanStage> rescaleStages(List<PlanStage> stages, int taskCount) {
        if (stages == null || stages.isEmpty()) {
            return List.of();
        }
        if (taskCount <= 0) {
            return List.of();
        }
        if (stages.size() == 1) {
            return stages;
        }
        int tasksPerStage = (int) Math.ceil((double) taskCount / stages.size());
        List<PlanStage> result = new ArrayList<>();
        int remaining = taskCount;
        for (int i = 0; i < stages.size() && remaining > 0; i++) {
            int take = Math.min(tasksPerStage, remaining);
            result.add(stages.get(i));
            remaining -= take;
        }
        return result;
    }
}
