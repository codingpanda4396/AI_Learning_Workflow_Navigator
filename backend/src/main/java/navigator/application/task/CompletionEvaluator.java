package navigator.application.task;

import navigator.domain.model.ExecutableTaskSpec;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 基于 evaluationRubric 的完成判定；无 rubric 时降级到最小规则。
 */
@Component
public class CompletionEvaluator {

    private static final int FALLBACK_MIN_CHARS_SELF_EXPLAIN = 35;
    private static final int FALLBACK_MIN_CHARS_CHECKPOINT = 12;

    public EvaluationResult evaluateSelfExplanation(TaskExecutionRuntime runtime,
                                                    ExecutableTaskSpec spec,
                                                    String userInput) {
        if (spec != null && spec.getEvaluationRubric() != null && spec.getEvaluationRubric().getDimensions() != null
                && !spec.getEvaluationRubric().getDimensions().isEmpty()) {
            return evaluateWithRubric(spec.getEvaluationRubric(), userInput);
        }
        return evaluateFallback(userInput, FALLBACK_MIN_CHARS_SELF_EXPLAIN, "自我解释");
    }

    public EvaluationResult evaluateCheckpoint(TaskExecutionRuntime runtime,
                                               ExecutableTaskSpec spec,
                                               String answer) {
        if (spec != null && spec.getEvaluationRubric() != null && spec.getEvaluationRubric().getDimensions() != null
                && !spec.getEvaluationRubric().getDimensions().isEmpty()) {
            return evaluateWithRubric(spec.getEvaluationRubric(), answer);
        }
        return evaluateFallback(answer, FALLBACK_MIN_CHARS_CHECKPOINT, "微检查");
    }

    private EvaluationResult evaluateWithRubric(ExecutableTaskSpec.EvaluationRubric rubric, String userInput) {
        if (userInput == null || userInput.isBlank()) {
            return EvaluationResult.builder()
                    .pass(false)
                    .reason("输入为空")
                    .missingDimensions(List.copyOf(rubric.getDimensions().keySet()))
                    .build();
        }
        String text = userInput.trim();
        List<String> missing = new ArrayList<>();
        for (Map.Entry<String, String> dim : rubric.getDimensions().entrySet()) {
            if (!dimensionSatisfied(dim.getKey(), dim.getValue(), text)) {
                missing.add(dim.getKey());
            }
        }
        String passThreshold = rubric.getPassThreshold();
        boolean pass = passesThreshold(missing, rubric.getDimensions().size(), passThreshold);
        return EvaluationResult.builder()
                .pass(pass)
                .reason(pass ? "满足" + passThreshold : "缺少：" + String.join("、", missing))
                .missingDimensions(missing)
                .build();
    }

    private boolean dimensionSatisfied(String dimensionName, String requirement, String text) {
        if (requirement == null || requirement.isEmpty()) return true;
        switch (dimensionName) {
            case "复述":
                return text.length() >= 15 && (text.contains("是") || text.contains("指") || text.contains("就是") || text.contains("即"));
            case "例子":
                return text.contains("例如") || text.contains("比如") || text.contains("如") || text.matches(".*[0-9一二三四].*");
            case "差异":
                return text.length() >= 12 && (text.contains("不同") || text.contains("区别") || text.contains("差异") || text.contains("比"));
            case "步骤":
                return text.length() >= 10;
            case "理由":
                return text.length() >= 8 && (text.contains("因为") || text.contains("所以") || text.contains("由于"));
            case "检查":
                return text.length() >= 8;
            default:
                return text.length() >= 10;
        }
    }

    private boolean passesThreshold(List<String> missing, int totalDims, String passThreshold) {
        if (missing.isEmpty()) return true;
        if (passThreshold == null) return false;
        if (passThreshold.contains("覆盖") && passThreshold.contains("/")) {
            try {
                String[] parts = passThreshold.replaceAll("[^0-9/]", "").split("/");
                if (parts.length >= 2) {
                    int required = Integer.parseInt(parts[0].trim());
                    int satisfied = totalDims - missing.size();
                    return satisfied >= required;
                }
            } catch (NumberFormatException ignored) {
            }
        }
        if ("覆盖1/1".equals(passThreshold) || passThreshold.contains("1/1")) {
            return missing.isEmpty();
        }
        return missing.isEmpty();
    }

    private EvaluationResult evaluateFallback(String input, int minChars, String label) {
        if (input == null || input.trim().length() < minChars) {
            return EvaluationResult.builder()
                    .pass(false)
                    .reason(label + "内容过短，请补充至至少" + minChars + "字")
                    .missingDimensions(List.of())
                    .build();
        }
        return EvaluationResult.builder()
                .pass(true)
                .reason("满足最小要求")
                .missingDimensions(List.of())
                .build();
    }
}
