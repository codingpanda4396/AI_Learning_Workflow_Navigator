package navigator.application.scaffold;

import navigator.api.dto.scaffold.TutorResponse;
import navigator.api.dto.scaffold.ValidationResult;
import org.springframework.stereotype.Component;

/**
 * Short feedback for reflection cards.
 */
@Component
public class ReflectionTutorComposer {

    public TutorResponse compose(String actionId, ValidationResult validation) {
        if (validation == null) {
            return TutorResponse.builder()
                    .feedbackType("ERROR")
                    .content("校验结果缺失。")
                    .canProceed(false)
                    .build();
        }
        if (validation.isPassed()) {
            boolean last = DfsBfsReflectionScaffoldDefinition.ACTION_CAPABILITY_NAME.equals(actionId);
            return TutorResponse.builder()
                    .feedbackType("PASS")
                    .content(last ? "这条能力表述已经可复用，准备生成反思沉淀。" : "这一张已经过关，继续下一张。")
                    .nextPrompt(last ? null : nextHint(actionId))
                    .canProceed(true)
                    .build();
        }

        String code = validation.getErrorType() != null ? validation.getErrorType() : "";
        String message = validation.getMessage() != null ? validation.getMessage() : "请写得更具体一点。";
        return TutorResponse.builder()
                .feedbackType("REJECT")
                .content(shortHint(code, message))
                .canProceed(false)
                .build();
    }

    private static String shortHint(String code, String fallback) {
        return switch (code) {
            case ReflectionErrorTypes.TOO_SHORT -> "这句还太短，撑不起迁移规则：" + fallback;
            case ReflectionErrorTypes.GENERIC -> "这句还太泛，请改成具体错误或具体根因：" + fallback;
            case ReflectionErrorTypes.DEFINITION_ONLY -> "这句还是定义复读，请改成场景判断句：" + fallback;
            case ReflectionErrorTypes.NO_RULE_SHAPE ->
                    "这句还不能提现迁移能力，请改成『遇到____时，我优先____，因为____』。";
            case ReflectionErrorTypes.CAPABILITY_VAGUE ->
                    "能力表述还不可检验，请改成『我能____』并写清能独立做到什么。";
            case ReflectionErrorTypes.INVALID_ACTION -> fallback;
            default -> fallback;
        };
    }

    private static String nextHint(String actionId) {
        if (DfsBfsReflectionScaffoldDefinition.ACTION_ERROR_RECALL.equals(actionId)) {
            return "下一张：说明这个错为什么会发生。";
        }
        if (DfsBfsReflectionScaffoldDefinition.ACTION_ROOT_CAUSE.equals(actionId)) {
            return "下一张：写以后怎么判断优先想到 DFS 还是 BFS。";
        }
        if (DfsBfsReflectionScaffoldDefinition.ACTION_DECISION_RULE.equals(actionId)) {
            return "下一张：写这次真正获得的能力。";
        }
        return "即将生成反思沉淀。";
    }
}
