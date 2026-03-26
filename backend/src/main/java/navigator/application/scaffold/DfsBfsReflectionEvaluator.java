package navigator.application.scaffold;

import navigator.api.dto.scaffold.ValidationResult;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 四张反思卡：长度、泛化句、定义复述、规则形状、能力命名。
 */
@Component
public class DfsBfsReflectionEvaluator implements ReflectionEvaluator {

    private static final int MIN_ERROR_RECALL = 18;
    private static final int MIN_ROOT_CAUSE = 16;
    private static final int MIN_DECISION = 28;
    private static final int MIN_CAPABILITY = 16;

    private static final Pattern RULE_SHAPE = Pattern.compile(
            "(当|如果|若|遇到|优先|需要|强调|场景|问题).{0,40}(dfs|bfs|深度|广度|层|队列|栈|先)",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    @Override
    public ValidationResult validate(StructureValidationContext ctx) {
        String aid = ctx.getActionId();
        String raw = ctx.getUserInput() != null ? ctx.getUserInput().trim() : "";
        String n = raw.toLowerCase(Locale.ROOT);

        if (DfsBfsReflectionScaffoldDefinition.ACTION_ERROR_RECALL.equals(aid)) {
            return validateErrorRecall(raw, n);
        }
        if (DfsBfsReflectionScaffoldDefinition.ACTION_ROOT_CAUSE.equals(aid)) {
            return validateRootCause(raw, n);
        }
        if (DfsBfsReflectionScaffoldDefinition.ACTION_DECISION_RULE.equals(aid)) {
            return validateDecisionRule(raw, n);
        }
        if (DfsBfsReflectionScaffoldDefinition.ACTION_CAPABILITY_NAME.equals(aid)) {
            return validateCapability(raw, n);
        }
        return ValidationResult.builder()
                .passed(false)
                .errorType(ReflectionErrorTypes.INVALID_ACTION)
                .message("未知的反思动作。")
                .suggestions(List.of())
                .build();
    }

    private static ValidationResult validateErrorRecall(String raw, String n) {
        if (raw.length() < MIN_ERROR_RECALL) {
            return fail(ReflectionErrorTypes.TOO_SHORT, "请写具体一点：你到底错在「表达」「机制」还是「因果链」中的哪一环。");
        }
        if (isGenericOnly(n) || isVagueLearn(n)) {
            return fail(ReflectionErrorTypes.GENERIC, "太泛了。请举一个你确实写错或想错过的点。");
        }
        return pass();
    }

    private static ValidationResult validateRootCause(String raw, String n) {
        if (raw.length() < MIN_ROOT_CAUSE) {
            return fail(ReflectionErrorTypes.TOO_SHORT, "根因请至少写清楚一句：是背定义、机制不清、因果断了，还是表达太模糊。");
        }
        if (isVagueLearn(n) && raw.length() < 40) {
            return fail(ReflectionErrorTypes.GENERIC, "请对应上一张的错误，说明根因，不要只写「基础不好」。");
        }
        return pass();
    }

    private static ValidationResult validateDecisionRule(String raw, String n) {
        if (raw.length() < MIN_DECISION) {
            return fail(ReflectionErrorTypes.TOO_SHORT, "规则写长一点：用「遇到什么情况 → 优先想到哪种搜索」。");
        }
        if (isDefinitionParrot(n)) {
            return fail(ReflectionErrorTypes.DEFINITION_ONLY, "不要只复述定义。请写清「什么特征 → 选 DFS 还是 BFS」。");
        }
        boolean hasDfs = n.contains("dfs") || n.contains("深度");
        boolean hasBfs = n.contains("bfs") || n.contains("广度") || n.contains("层");
        if (!RULE_SHAPE.matcher(raw).find() && !(hasDfs && hasBfs)) {
            return fail(ReflectionErrorTypes.NO_RULE_SHAPE, "加一句条件句：例如「当…优先…」并点出 DFS/BFS 或深度/广度特征。");
        }
        return pass();
    }

    private static ValidationResult validateCapability(String raw, String n) {
        if (raw.length() < MIN_CAPABILITY) {
            return fail(ReflectionErrorTypes.TOO_SHORT, "能力请写具体：你能「判断 / 解释 / 对比」什么。");
        }
        if (n.matches(".*(学会了|掌握了)\\s*(dfs|bfs|深度|广度).*") && raw.length() < 45) {
            return fail(ReflectionErrorTypes.CAPABILITY_VAGUE, "避免「学会了 DFS/BFS」。改成你能独立做到的一件事。");
        }
        if ((n.contains("学会了") || n.contains("掌握了")) && !n.contains("能") && raw.length() < 50) {
            return fail(ReflectionErrorTypes.CAPABILITY_VAGUE, "用「我能…」写一条可检验的能力。");
        }
        return pass();
    }

    private static boolean isGenericOnly(String n) {
        return n.length() < 25 && (n.contains("不会") || n.contains("不懂") || n.contains("不深"));
    }

    private static boolean isVagueLearn(String n) {
        return n.contains("不太会") || n.contains("基础不好") || n.contains("理解不深") || n.contains("没学会");
    }

    private static boolean isDefinitionParrot(String n) {
        boolean mentionsBoth =
                (n.contains("dfs") || n.contains("深度")) && (n.contains("bfs") || n.contains("广度"));
        boolean hasCondition =
                n.contains("当") || n.contains("如果") || n.contains("若") || n.contains("优先") || n.contains("遇到");
        return mentionsBoth && !hasCondition && n.length() < 120;
    }

    private static ValidationResult fail(String errorType, String message) {
        return ValidationResult.builder()
                .passed(false)
                .errorType(errorType)
                .message(message)
                .suggestions(List.of())
                .build();
    }

    private static ValidationResult pass() {
        return ValidationResult.builder()
                .passed(true)
                .message("通过")
                .suggestions(List.of())
                .build();
    }
}
