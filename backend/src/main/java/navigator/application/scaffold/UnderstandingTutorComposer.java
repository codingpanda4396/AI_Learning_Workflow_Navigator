package navigator.application.scaffold;

import navigator.api.dto.scaffold.TutorResponse;
import navigator.api.dto.scaffold.ValidationResult;
import org.springframework.stereotype.Component;

/**
 * UNDERSTANDING 阶段 Tutor 文案：与校验器解耦、短模板、无 LLM。
 */
@Component
public class UnderstandingTutorComposer {

    public TutorResponse compose(String actionId, ValidationResult v) {
        if (v.isPassed()) {
            return TutorResponse.builder()
                    .feedbackType("PASS")
                    .content("可以。这一段的机制叙述到位。")
                    .nextPrompt(nextHint(actionId))
                    .canProceed(true)
                    .build();
        }
        String err = v.getErrorType() != null ? v.getErrorType() : "FAIL";
        return switch (err) {
            case "INSUFFICIENT_CONTENT" -> TutorResponse.builder()
                    .feedbackType("PROMPT_NUDGE")
                    .content(v.getMessage())
                    .nextPrompt("再写几句：把「起点—推进—回退/层」串起来。")
                    .canProceed(false)
                    .build();
            case "BOUNDARY_VIOLATION" -> TutorResponse.builder()
                    .feedbackType("LOCAL_CORRECTION")
                    .content(v.getMessage())
                    .nextPrompt("用自然语言描述过程即可。")
                    .canProceed(false)
                    .build();
            case "SHALLOW_UNDERSTANDING" -> TutorResponse.builder()
                    .feedbackType("ASK_REFRAME")
                    .content(v.getMessage())
                    .nextPrompt("不要停留在定义，请按「步骤」说明推进。")
                    .canProceed(false)
                    .build();
            case "MISSING_SLOT" -> TutorResponse.builder()
                    .feedbackType("PROMPT_NUDGE")
                    .content(v.getMessage())
                    .nextPrompt(formatMissing(v))
                    .canProceed(false)
                    .build();
            default -> TutorResponse.builder()
                    .feedbackType("LOCAL_CORRECTION")
                    .content(v.getMessage())
                    .nextPrompt("对照上面的缺口补一句。")
                    .canProceed(false)
                    .build();
        };
    }

    private static String formatMissing(ValidationResult v) {
        if (v.getMissingAspects() != null && !v.getMissingAspects().isEmpty()) {
            return "先补：「" + String.join("」「", v.getMissingAspects()) + "」。";
        }
        return "请补全缺失的机制环节。";
    }

    private static String nextHint(String actionId) {
        if (DfsBfsUnderstandingScaffoldDefinition.ACTION_DFS_STEPS.equals(actionId)) {
            return "下一张：说明 BFS 为何按层推进。";
        }
        return "UNDERSTANDING 完成，可进入探索对话。";
    }
}
