package navigator.application.scaffold;

import navigator.api.dto.scaffold.TutorResponse;
import navigator.api.dto.scaffold.ValidationResult;
import org.springframework.stereotype.Component;

/**
 * Short, gap-focused tutor responses for the understanding stage.
 */
@Component
public class UnderstandingTutorComposer {

    public TutorResponse compose(String actionId, ValidationResult validation) {
        if (validation.isPassed()) {
            return TutorResponse.builder()
                    .feedbackType("PASS")
                    .content("这次推进链已经完整了，可以进入下一步。")
                    .nextPrompt(nextHint(actionId))
                    .canProceed(true)
                    .build();
        }

        String error = validation.getErrorType() != null ? validation.getErrorType() : "FAIL";
        return switch (error) {
            case "INSUFFICIENT_CONTENT" -> TutorResponse.builder()
                    .feedbackType("PROMPT_NUDGE")
                    .content("你现在写得太短了，还看不出完整推进链。")
                    .nextPrompt("补成完整推进链：起点 -> 扩展 -> 回退 / 顺序意义。")
                    .canProceed(false)
                    .build();
            case "BOUNDARY_VIOLATION" -> TutorResponse.builder()
                    .feedbackType("LOCAL_CORRECTION")
                    .content("你写到了越界内容，这一阶段只讲过程，不讲代码和复杂度。")
                    .nextPrompt("删掉越界内容，只保留推进过程。")
                    .canProceed(false)
                    .build();
            case "SHALLOW_UNDERSTANDING" -> TutorResponse.builder()
                    .feedbackType("ASK_REFRAME")
                    .content("你已经说到了名字，但还没说清过程。")
                    .nextPrompt("不要停留在定义标签，请按步骤把推进过程讲出来。")
                    .canProceed(false)
                    .build();
            case "MISSING_SLOT" -> TutorResponse.builder()
                    .feedbackType("PROMPT_NUDGE")
                    .content("你已经说到了一部分，但还有关键槽位没补齐。")
                    .nextPrompt(formatMissing(validation))
                    .canProceed(false)
                    .build();
            default -> TutorResponse.builder()
                    .feedbackType("LOCAL_CORRECTION")
                    .content("这次还没过，请按缺口重写。")
                    .nextPrompt("对照缺口补成完整推进链。")
                    .canProceed(false)
                    .build();
        };
    }

    private static String formatMissing(ValidationResult validation) {
        if (validation.getMissingAspects() != null && !validation.getMissingAspects().isEmpty()) {
            return "先补清这些缺口：" + String.join("、", validation.getMissingAspects());
        }
        return "请补全缺失的机制环节。";
    }

    private static String nextHint(String actionId) {
        if (DfsBfsUnderstandingScaffoldDefinition.ACTION_DFS_STEPS.equals(actionId)) {
            return "下一张：说明 BFS 为什么按层推进。";
        }
        return "UNDERSTANDING 完成，进入训练。";
    }
}
