package navigator.application.scaffold;

import navigator.api.dto.scaffold.TutorResponse;
import navigator.api.dto.scaffold.TrainingDetectedProblem;
import navigator.api.dto.scaffold.TrainingFeedback;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

/**
 * 将 {@link TrainingFeedback} 转为短反馈文案，不输出满分标准答案。
 */
@Component
public class TrainingTutorComposer {

    public TutorResponse compose(String actionId, TrainingFeedback feedback) {
        if (feedback == null) {
            return TutorResponse.builder()
                    .feedbackType("ERROR")
                    .content("系统未能生成反馈。")
                    .canProceed(false)
                    .build();
        }
        if (feedback.isCanProceed()) {
            return TutorResponse.builder()
                    .feedbackType("PASS")
                    .content("本轮表达达到要求。可以进入下一步。")
                    .nextPrompt(nextHint(actionId))
                    .canProceed(true)
                    .build();
        }
        StringBuilder sb = new StringBuilder();
        sb.append("本轮需要重构表达。");
        if (feedback.getDetectedProblems() != null && !feedback.getDetectedProblems().isEmpty()) {
            sb.append(" 关注点：");
            String joined = feedback.getDetectedProblems().stream()
                    .map(TrainingDetectedProblem::getProblemText)
                    .collect(Collectors.joining(" "));
            sb.append(joined);
        }
        String next = feedback.getRevisionInstruction() != null && !feedback.getRevisionInstruction().isBlank()
                ? feedback.getRevisionInstruction()
                : "请按提示改写，不要直接索要完整答案。";
        return TutorResponse.builder()
                .feedbackType("REVISION")
                .content(sb.toString().trim())
                .nextPrompt(next)
                .canProceed(false)
                .build();
    }

    private static String nextHint(String actionId) {
        if (DfsBfsTrainingScaffoldDefinition.ACTION_BFS_SHORTEST.equals(actionId)) {
            return "下一张：说明 DFS 与 BFS 的顺序差异带来的结果差异。";
        }
        if (DfsBfsTrainingScaffoldDefinition.ACTION_ORDER_CONSEQUENCE.equals(actionId)) {
            return "完成训练后进入反思收敛。";
        }
        return "继续下一步。";
    }
}
