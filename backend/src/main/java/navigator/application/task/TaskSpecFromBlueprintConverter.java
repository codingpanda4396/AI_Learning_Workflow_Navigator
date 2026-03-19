package navigator.application.task;

import navigator.domain.enums.ScaffoldIntensity;
import navigator.domain.enums.TaskType;
import navigator.domain.enums.TimeBudget;
import navigator.domain.model.ExecutableTaskSpec;
import navigator.domain.model.LearnerStrategyProfile;
import navigator.domain.model.TaskBlueprint;
import navigator.domain.model.TimeBudgetConstraint;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 将 TaskBlueprint 转换为 ExecutableTaskSpec。
 */
@Component
public class TaskSpecFromBlueprintConverter {

    public ExecutableTaskSpec convert(TaskBlueprint bp,
                                      LearnerStrategyProfile strategyProfile,
                                      TimeBudgetConstraint timeBudgetConstraint) {
        if (bp == null) {
            return null;
        }
        TimeBudget budget = timeBudgetConstraint != null && timeBudgetConstraint.getTimeBudget() != null
                ? timeBudgetConstraint.getTimeBudget() : TimeBudget.WITHIN_60_MIN;
        ExecutableTaskSpec.TimeBoxPolicy timeBoxPolicy = ExecutableTaskSpec.defaultTimeBoxPolicy(budget);
        ExecutableTaskSpec.ScaffoldPolicy scaffoldPolicy = buildScaffoldPolicy(strategyProfile);
        ExecutableTaskSpec.EvaluationRubric rubric = evaluationRubricForType(bp.getTaskType());

        return ExecutableTaskSpec.builder()
                .taskId(bp.getTaskId())
                .taskType(bp.getTaskType())
                .title(bp.getTitle())
                .estimatedMinutes(bp.getEstimatedMinutes())
                .timeBoxPolicy(timeBoxPolicy)
                .completionCriteria(bp.getCompletionCriteria())
                .evidenceToCollect(bp.getEvidenceToCollect())
                .evaluationRubric(rubric)
                .scaffoldPolicy(scaffoldPolicy)
                .build();
    }

    private ExecutableTaskSpec.ScaffoldPolicy buildScaffoldPolicy(LearnerStrategyProfile profile) {
        int maxExplore = 3;
        int maxRemedial = 2;
        if (profile != null && profile.getScaffoldIntensity() != null) {
            if (profile.getScaffoldIntensity() == ScaffoldIntensity.LIGHT) {
                maxExplore = 1;
                maxRemedial = 1;
            } else if (profile.getScaffoldIntensity() == ScaffoldIntensity.STRICT) {
                maxExplore = 4;
                maxRemedial = 3;
            }
        }
        return ExecutableTaskSpec.ScaffoldPolicy.builder()
                .enableOrient(true)
                .enableExplore(true)
                .enableSelfExplain(true)
                .enableCheckpoint(true)
                .maxExploreTurns(maxExplore)
                .maxRemedialTurns(maxRemedial)
                .build();
    }

    private ExecutableTaskSpec.EvaluationRubric evaluationRubricForType(TaskType type) {
        if (type == null) {
            return ExecutableTaskSpec.EvaluationRubric.builder()
                    .dimensions(Map.of("完成", "满足任务目标"))
                    .passThreshold("覆盖1/1")
                    .build();
        }
        Map<String, String> dimensions = new HashMap<>();
        String passThreshold;
        switch (type) {
            case CONCEPT_EXPLAIN:
                dimensions.put("复述", "能用自己的话说出定义");
                dimensions.put("例子", "能举出至少一个例子");
                passThreshold = "覆盖2/2";
                break;
            case COMPARE_AND_CONNECT:
                dimensions.put("差异", "能说出至少2个差异或联系");
                passThreshold = "覆盖1/1";
                break;
            case GUIDED_EXAMPLE:
                dimensions.put("步骤", "能讲清关键步骤");
                passThreshold = "覆盖1/1";
                break;
            case SELF_EXPLANATION:
                dimensions.put("复述", "能独立复述不依赖原文");
                passThreshold = "覆盖1/1";
                break;
            case MICRO_PRACTICE:
                dimensions.put("理由", "能说明为什么这样做");
                passThreshold = "覆盖1/1";
                break;
            case CHECKPOINT_REVIEW:
                dimensions.put("检查", "能通过本阶段关键检查点");
                passThreshold = "覆盖1/1";
                break;
            default:
                dimensions.put("完成", "满足任务目标");
                passThreshold = "覆盖1/1";
        }
        return ExecutableTaskSpec.EvaluationRubric.builder()
                .dimensions(dimensions)
                .passThreshold(passThreshold)
                .build();
    }
}
