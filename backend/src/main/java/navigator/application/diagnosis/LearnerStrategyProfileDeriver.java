package navigator.application.diagnosis;

import navigator.domain.enums.FeedbackStyle;
import navigator.domain.enums.LearningPreference;
import navigator.domain.enums.ScaffoldIntensity;
import navigator.domain.enums.TaskType;
import navigator.domain.model.GoalContextSnapshot;
import navigator.domain.model.LearnerProfileSnapshot;
import navigator.domain.model.LearnerStrategyProfile;
import navigator.domain.model.StructuredLearningGoal;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 策略画像派生（规则）：从 LearnerProfileSnapshot + GoalContextSnapshot/Goal 生成最小可用策略合同。
 *
 * 后续可以逐步增强，但必须保持“无 LLM 也可运行”。
 */
@Component
public class LearnerStrategyProfileDeriver {

    public LearnerStrategyProfile derive(String diagnosisId,
                                         LearnerProfileSnapshot profile,
                                         StructuredLearningGoal goal,
                                         GoalContextSnapshot goalContextSnapshot) {
        ScaffoldIntensity scaffoldIntensity = ScaffoldIntensity.STANDARD;
        FeedbackStyle feedbackStyle = FeedbackStyle.COACHING;
        Integer checkpointFrequency = 2;

        if (profile != null) {
            // 执行稳定性差/风险较高 → 更强脚手架
            if (profile.getExecutionStability() != null) {
                switch (profile.getExecutionStability()) {
                    case UNSTABLE:
                        scaffoldIntensity = ScaffoldIntensity.STRICT;
                        checkpointFrequency = 1;
                        break;
                    case STABLE:
                        scaffoldIntensity = ScaffoldIntensity.LIGHT;
                        checkpointFrequency = 3;
                        break;
                    default:
                        break;
                }
            }
            // 时间预算紧 → 更直接反馈（少绕弯）
            if (profile.getTimeBudgetLevel() != null) {
                switch (profile.getTimeBudgetLevel()) {
                    case WITHIN_15_MIN:
                        feedbackStyle = FeedbackStyle.DIRECT;
                        break;
                    default:
                        break;
                }
            }
            // 学习偏好 → 任务类型顺序倾向
            if (profile.getLearningPreference() != null) {
                if (profile.getLearningPreference() == LearningPreference.PRACTICE_FIRST) {
                    scaffoldIntensity = scaffoldIntensity == ScaffoldIntensity.LIGHT ? ScaffoldIntensity.STANDARD : scaffoldIntensity;
                }
            }
        }

        List<TaskType> preferred = preferredTaskTypes(profile != null ? profile.getLearningPreference() : null);
        List<String> riskMitigation = new ArrayList<>();
        if (goalContextSnapshot != null && goalContextSnapshot.getRiskTags() != null) {
            riskMitigation.addAll(goalContextSnapshot.getRiskTags());
        }
        if (profile != null && profile.getRiskTags() != null) {
            for (String t : profile.getRiskTags()) {
                if (!riskMitigation.contains(t)) {
                    riskMitigation.add(t);
                }
            }
        }

        return LearnerStrategyProfile.builder()
                .diagnosisId(diagnosisId)
                .preferredTaskTypes(preferred)
                .scaffoldIntensity(scaffoldIntensity)
                .feedbackStyle(feedbackStyle)
                .checkpointFrequency(checkpointFrequency)
                .riskMitigationTags(riskMitigation)
                .build();
    }

    private static List<TaskType> preferredTaskTypes(LearningPreference pref) {
        if (pref == null) {
            return List.of(TaskType.CONCEPT_EXPLAIN, TaskType.GUIDED_EXAMPLE, TaskType.MICRO_PRACTICE);
        }
        switch (pref) {
            case CONCEPT_FIRST:
            case FRAMEWORK_FIRST:
                return List.of(TaskType.CONCEPT_EXPLAIN, TaskType.COMPARE_AND_CONNECT, TaskType.GUIDED_EXAMPLE, TaskType.MICRO_PRACTICE);
            case EXAMPLE_FIRST:
                return List.of(TaskType.GUIDED_EXAMPLE, TaskType.CONCEPT_EXPLAIN, TaskType.MICRO_PRACTICE);
            case PRACTICE_FIRST:
                return List.of(TaskType.MICRO_PRACTICE, TaskType.CHECKPOINT_REVIEW, TaskType.SELF_EXPLANATION);
            case CORE_CONTRAST_FIRST:
                return List.of(TaskType.COMPARE_AND_CONNECT, TaskType.CONCEPT_EXPLAIN, TaskType.GUIDED_EXAMPLE);
            case STEP_BY_STEP:
                return List.of(TaskType.GUIDED_EXAMPLE, TaskType.SELF_EXPLANATION, TaskType.CHECKPOINT_REVIEW);
            case BALANCED:
            default:
                return List.of(TaskType.CONCEPT_EXPLAIN, TaskType.GUIDED_EXAMPLE, TaskType.SELF_EXPLANATION, TaskType.MICRO_PRACTICE);
        }
    }
}

