package navigator.application.scaffold;

import navigator.api.dto.scaffold.TutorResponse;
import navigator.api.dto.scaffold.ValidationResult;
import org.springframework.stereotype.Component;

/**
 * 反思阶段：单点短提示，不做长篇讲解。
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
                    .content(last ? "可以。将生成反思沉淀并进入探索。" : "可以。继续下一张。")
                    .nextPrompt(last ? null : nextHint(actionId))
                    .canProceed(true)
                    .build();
        }
        String code = validation.getErrorType() != null ? validation.getErrorType() : "";
        String msg = validation.getMessage() != null ? validation.getMessage() : "请写得更具体一点。";
        return TutorResponse.builder()
                .feedbackType("REJECT")
                .content(shortHint(code, msg))
                .canProceed(false)
                .build();
    }

    private static String shortHint(String code, String fallback) {
        return switch (code) {
            case ReflectionErrorTypes.TOO_SHORT -> "再展开一点：" + fallback;
            case ReflectionErrorTypes.GENERIC -> "请落到具体点：" + fallback;
            case ReflectionErrorTypes.DEFINITION_ONLY -> "别停在定义：" + fallback;
            case ReflectionErrorTypes.NO_RULE_SHAPE -> "补上判断形状：" + fallback;
            case ReflectionErrorTypes.CAPABILITY_VAGUE -> "能力要可检验：" + fallback;
            case ReflectionErrorTypes.INVALID_ACTION -> fallback;
            default -> fallback;
        };
    }

    private static String nextHint(String actionId) {
        if (DfsBfsReflectionScaffoldDefinition.ACTION_ERROR_RECALL.equals(actionId)) {
            return "下一张：说明根因。";
        }
        if (DfsBfsReflectionScaffoldDefinition.ACTION_ROOT_CAUSE.equals(actionId)) {
            return "下一张：写可迁移判断规则。";
        }
        if (DfsBfsReflectionScaffoldDefinition.ACTION_DECISION_RULE.equals(actionId)) {
            return "下一张：给能力命名。";
        }
        return "即将生成反思沉淀。";
    }
}
