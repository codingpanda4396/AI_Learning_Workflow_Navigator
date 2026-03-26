package navigator.application.scaffold;

import navigator.api.dto.scaffold.ValidationResult;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * DFS/BFS 的 UNDERSTANDING：槽位覆盖 + 浅层复述检测。
 */
@Component
public class DfsBfsUnderstandingValidator implements StructureValidator {

    public static final String PACK_ID = "ds_dfs_bfs";
    public static final String STAGE_KEY = DfsBfsUnderstandingScaffoldDefinition.STAGE_KEY;

    private static final int MIN_CHARS = 36;
    private static final Pattern CODEISH = Pattern.compile(
            "(public\\s+static|void\\s+main|import\\s+java|\\bdef\\b|\\bint\\s+\\w+\\s*[=;]|for\\s*\\(|while\\s*\\(|#include)",
            Pattern.CASE_INSENSITIVE);

    @Override
    public ValidationResult validate(StructureValidationContext ctx) {
        String raw = ctx.getUserInput() != null ? ctx.getUserInput().trim() : "";
        if (raw.length() < MIN_CHARS) {
            return ValidationResult.builder()
                    .passed(false)
                    .errorType("INSUFFICIENT_CONTENT")
                    .message("先用多几句话把机制讲清楚，不要一句话带过。")
                    .suggestions(List.of("写清起点、推进、回退", "避免只下定义"))
                    .matchedAspects(List.of())
                    .missingAspects(List.of("内容过短"))
                    .build();
        }
        if (CODEISH.matcher(raw).find()) {
            return ValidationResult.builder()
                    .passed(false)
                    .errorType("BOUNDARY_VIOLATION")
                    .message("当前阶段只谈机制叙述，请不要贴代码片段。")
                    .suggestions(List.of("用自然语言描述推进过程"))
                    .matchedAspects(List.of())
                    .missingAspects(List.of("去掉代码"))
                    .build();
        }

        String norm = raw.toLowerCase(Locale.ROOT);
        if (norm.contains("o(") || norm.contains("复杂度") || raw.contains("时间复杂度") || raw.contains("空间复杂度")) {
            return ValidationResult.builder()
                    .passed(false)
                    .errorType("BOUNDARY_VIOLATION")
                    .message("先不要展开复杂度推导，专注讲清推进过程。")
                    .suggestions(List.of("删掉复杂度相关表述"))
                    .matchedAspects(List.of())
                    .missingAspects(List.of("避免复杂度"))
                    .build();
        }

        String actionId = ctx.getActionId();
        if (DfsBfsUnderstandingScaffoldDefinition.ACTION_DFS_STEPS.equals(actionId)) {
            return validateDfs(raw);
        }
        if (DfsBfsUnderstandingScaffoldDefinition.ACTION_BFS_LAYERS.equals(actionId)) {
            return validateBfs(raw);
        }
        return ValidationResult.builder()
                .passed(false)
                .errorType("INVALID_ACTION")
                .message("未知的动作卡。")
                .suggestions(List.of())
                .build();
    }

    private static ValidationResult validateDfs(String raw) {
        PseudoUnderstandingDetector.DetectionResult d = PseudoUnderstandingDetector.detectDfs(raw);
        if (d.shallowUnderstanding()) {
            return ValidationResult.builder()
                    .passed(false)
                    .errorType("SHALLOW_UNDERSTANDING")
                    .message("你提到了名称，但还没有解释 DFS 是如何一步步推进与回退的。")
                    .suggestions(List.of(
                            "写清：从哪里出发",
                            "下一步怎么选未访问的走向",
                            "走不通时如何退回"
                    ))
                    .matchedAspects(d.matchedLabels())
                    .missingAspects(d.missingSlots())
                    .build();
        }
        if (!d.missingSlots().isEmpty()) {
            return ValidationResult.builder()
                    .passed(false)
                    .errorType("MISSING_SLOT")
                    .message("还缺这些关键环节：" + String.join("、", d.missingSlots()))
                    .suggestions(d.missingSlots())
                    .matchedAspects(d.matchedLabels())
                    .missingAspects(d.missingSlots())
                    .build();
        }
        return ValidationResult.builder()
                .passed(true)
                .errorType(null)
                .message("通过")
                .suggestions(List.of())
                .matchedAspects(d.matchedLabels())
                .missingAspects(List.of())
                .build();
    }

    private static ValidationResult validateBfs(String raw) {
        PseudoUnderstandingDetector.DetectionResult d = PseudoUnderstandingDetector.detectBfs(raw);
        if (d.shallowUnderstanding()) {
            return ValidationResult.builder()
                    .passed(false)
                    .errorType("SHALLOW_UNDERSTANDING")
                    .message("你给出了名称，但还没有解释 BFS 是如何按层推进以及这种顺序的意义。")
                    .suggestions(List.of(
                            "写清：从哪出发",
                            "同层/下一层如何扩展",
                            "这种顺序为什么常与「更近先访问」有关"
                    ))
                    .matchedAspects(d.matchedLabels())
                    .missingAspects(d.missingSlots())
                    .build();
        }
        if (!d.missingSlots().isEmpty()) {
            return ValidationResult.builder()
                    .passed(false)
                    .errorType("MISSING_SLOT")
                    .message("还缺这些关键环节：" + String.join("、", d.missingSlots()))
                    .suggestions(d.missingSlots())
                    .matchedAspects(d.matchedLabels())
                    .missingAspects(d.missingSlots())
                    .build();
        }
        return ValidationResult.builder()
                .passed(true)
                .errorType(null)
                .message("通过")
                .suggestions(List.of())
                .matchedAspects(d.matchedLabels())
                .missingAspects(List.of())
                .build();
    }
}
