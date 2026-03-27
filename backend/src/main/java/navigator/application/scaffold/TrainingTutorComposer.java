package navigator.application.scaffold;

import navigator.api.dto.scaffold.TrainingDetectedProblem;
import navigator.api.dto.scaffold.TrainingFeedback;
import navigator.api.dto.scaffold.TutorResponse;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

/**
 * Converts training feedback into short rewrite-oriented tutor responses.
 */
@Component
public class TrainingTutorComposer {

    public TutorResponse compose(String actionId, TrainingFeedback feedback) {
        if (feedback == null) {
            return TutorResponse.builder()
                    .feedbackType("ERROR")
                    .content("系统暂时无法生成训练反馈。")
                    .canProceed(false)
                    .build();
        }
        if (feedback.isCanProceed()) {
            return TutorResponse.builder()
                    .feedbackType("PASS")
                    .content("这轮表达已经达标，可以进入下一步。")
                    .nextPrompt(nextHint(actionId))
                    .canProceed(true)
                    .build();
        }

        String problems = feedback.getDetectedProblems() == null
                ? ""
                : feedback.getDetectedProblems().stream()
                .map(TrainingDetectedProblem::getProblemText)
                .collect(Collectors.joining(" "));

        String nextPrompt = feedback.getRevisionInstruction() != null && !feedback.getRevisionInstruction().isBlank()
                ? feedback.getRevisionInstruction()
                : "请保留你的结论，只重写这 1 处因果链。";

        return TutorResponse.builder()
                .feedbackType("REVISION")
                .content(("这次不是答案错，而是有关键因果链没说出来。 缺口：" + problems).trim())
                .nextPrompt(nextPrompt)
                .canProceed(false)
                .build();
    }

    private static String nextHint(String actionId) {
        if (DfsBfsTrainingScaffoldDefinition.ACTION_BFS_SHORTEST.equals(actionId)) {
            return "下一张：说明 DFS 和 BFS 的顺序差异会带来什么结果差异。";
        }
        if (DfsBfsTrainingScaffoldDefinition.ACTION_ORDER_CONSEQUENCE.equals(actionId)) {
            return "训练完成后进入反思沉淀。";
        }
        return "继续下一步。";
    }
}
