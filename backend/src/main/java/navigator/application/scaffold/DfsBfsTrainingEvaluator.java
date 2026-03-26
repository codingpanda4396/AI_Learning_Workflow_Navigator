package navigator.application.scaffold;

import navigator.api.dto.scaffold.TrainingDetectedProblem;
import navigator.api.dto.scaffold.TrainingFeedback;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * DFS/BFS TRAINING：规则主导的最小机制检测（每轮最多 2 条问题）。
 */
@Component
public class DfsBfsTrainingEvaluator implements TrainingEvaluator {

    private static final int MIN_CHARS = 40;
    private static final int MAX_PROBLEMS = 2;
    private static final Pattern CODEISH = Pattern.compile(
            "(public\\s+static|void\\s+main|import\\s+java|\\bdef\\b|for\\s*\\(|while\\s*\\(|#include)",
            Pattern.CASE_INSENSITIVE);

    @Override
    public TrainingFeedback evaluate(StructureValidationContext ctx) {
        String raw = ctx.getUserInput() != null ? ctx.getUserInput().trim() : "";
        String actionId = ctx.getActionId();
        if (DfsBfsTrainingScaffoldDefinition.ACTION_BFS_SHORTEST.equals(actionId)) {
            return evaluateBfsShortest(raw);
        }
        if (DfsBfsTrainingScaffoldDefinition.ACTION_ORDER_CONSEQUENCE.equals(actionId)) {
            return evaluateOrderConsequence(raw);
        }
        return TrainingFeedback.builder()
                .canProceed(false)
                .revisionInstruction("未知的训练动作。")
                .detectedProblems(List.of(
                        TrainingDetectedProblem.builder()
                                .problemText("系统无法识别当前训练卡。")
                                .errorType(TrainingErrorTypes.MECHANISM_ERROR)
                                .evidence("")
                                .fixHint("请刷新页面后重试。")
                                .build()
                ))
                .errorTypes(List.of(TrainingErrorTypes.MECHANISM_ERROR))
                .build();
    }

    private static TrainingFeedback evaluateBfsShortest(String raw) {
        if (raw.length() < MIN_CHARS) {
            return failWith(List.of(missingStep("内容过短，因果链展不开。", "先写满几句话说清层进与距离顺序。")),
                    TrainingErrorTypes.MISSING_STEP,
                    "把回答写长一些，至少用多句把「层进—更近先访问—首次到达」串起来。");
        }
        if (CODEISH.matcher(raw).find()) {
            return failWith(List.of(vague("请不要贴代码片段。", "用自然语言描述过程。")),
                    TrainingErrorTypes.VAGUE_EXPRESSION,
                    "删掉代码，用口语把机制讲清楚。");
        }
        String n = raw.toLowerCase(Locale.ROOT);
        boolean layer = matchesAny(n, "层", "按层", "逐层", "一圈", "水波", "波纹", "扩散", "layer");
        boolean nearer = matchesAny(n, "更近", "距离", "离起点", "先访问", "近的先", "由近");
        boolean firstShortest = n.contains("最短")
                || (matchesAny(n, "第一次", "最先", "首次")
                && (n.contains("到达") || n.contains("碰到") || n.contains("遇见") || n.contains("访问") || n.contains("到")));
        boolean bfsName = n.contains("bfs") || n.contains("广度");

        List<TrainingDetectedProblem> problems = new ArrayList<>();
        if (!layer) {
            problems.add(missingStep("未清楚说明 BFS 如何按层/一圈圈扩展。", "补一句：它是如何一层层推开搜索边界的。"));
        }
        if (!nearer && !firstShortest) {
            problems.add(causalGap("未把「更近先被看到」与搜索顺序联系起来。", "说明为什么近的点会更早出现在 BFS 的访问顺序里。"));
        }
        if (!firstShortest) {
            problems.add(missingStep("未说明「第一次到达目标」与最短路径的关系。", "点出：在无权边下，先到达对应最短跳数。"));
        }

        if (bfsName && (countTrue(layer, nearer || firstShortest) < 2)) {
            TrainingDetectedProblem shallow = vague(
                    "像在背「广度优先」标签，缺少层进与首次到达的机制链。",
                    "用因果句连接：层扩展 → 更近先访问 → 首次碰到终点。");
            problems.add(0, shallow);
        }

        problems = trim(problems);
        if (problems.isEmpty()) {
            return TrainingFeedback.builder()
                    .canProceed(true)
                    .detectedProblems(List.of())
                    .errorTypes(List.of())
                    .revisionInstruction("")
                    .build();
        }
        return failFromProblems(problems, "请按上面 1～2 点重构表达：补因果，不要只下定义。");
    }

    private static TrainingFeedback evaluateOrderConsequence(String raw) {
        if (raw.length() < MIN_CHARS) {
            return failWith(List.of(missingStep("内容过短，难以判断顺序与结果的关系。", "多写几句对比两种推进方式。")),
                    TrainingErrorTypes.MISSING_STEP,
                    "写长一些，把顺序差异和结果差异绑在一起说。");
        }
        if (CODEISH.matcher(raw).find()) {
            return failWith(List.of(vague("请不要贴代码。", "用自然语言对比。")),
                    TrainingErrorTypes.VAGUE_EXPRESSION,
                    "删掉代码，用叙述对比 DFS 与 BFS。");
        }
        String n = raw.toLowerCase(Locale.ROOT);
        boolean orderDiff = matchesAny(n, "深度", "广度", "dfs", "bfs", "深入", "先深", "先广", "逐层", "一路", "回溯", "栈", "队列")
                && (n.contains("不同") || n.contains("区别") || n.contains("差异") || n.contains("不一样") || n.contains("相反"));
        boolean pathOrProcess = matchesAny(n, "路径", "结果", "找到", "搜索", "过程", "先碰到", "第一次");
        boolean scenario = matchesAny(n, "适用", "场景", "问题", "类型", "用途", "更适合", "什么时候");

        boolean onlyLabels = (n.contains("深度优先") || n.contains("dfs")) && (n.contains("广度优先") || n.contains("bfs"))
                && raw.length() < 90 && !pathOrProcess;

        List<TrainingDetectedProblem> problems = new ArrayList<>();
        if (!orderDiff) {
            problems.add(missingStep("未明确对比两者推进顺序的差异。", "各用一句话说明「先往哪走、如何扩展」。"));
        }
        if (!pathOrProcess) {
            problems.add(causalGap("未说明顺序差异如何影响找到的路径或搜索过程。", "点出：先深/先广会影响先碰到谁、路径形态等。"));
        }
        if (!scenario && problems.size() < 2) {
            problems.add(mechanismError("未落到适用问题或场景差异。", "各举一类更合适的用途（不必展开证明）。"));
        }
        if (onlyLabels) {
            problems.add(0, vague("主要在复述两个名称，缺少顺序→结果的因果。", "用「因为推进顺序不同，所以…」改写。"));
        }

        problems = trim(problems);
        if (problems.isEmpty()) {
            return TrainingFeedback.builder()
                    .canProceed(true)
                    .detectedProblems(List.of())
                    .errorTypes(List.of())
                    .revisionInstruction("")
                    .build();
        }
        return failFromProblems(problems, "请重构：先对比顺序，再写对路径/过程的影响，最后点适用场景。");
    }

    private static List<TrainingDetectedProblem> trim(List<TrainingDetectedProblem> problems) {
        if (problems.size() <= MAX_PROBLEMS) {
            return problems;
        }
        return new ArrayList<>(problems.subList(0, MAX_PROBLEMS));
    }

    private static boolean matchesAny(String normalized, String... needles) {
        for (String needle : needles) {
            if (normalized.contains(needle.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private static int countTrue(boolean... xs) {
        int c = 0;
        for (boolean x : xs) {
            if (x) {
                c++;
            }
        }
        return c;
    }

    private static TrainingFeedback failFromProblems(List<TrainingDetectedProblem> problems, String revision) {
        problems = trim(problems);
        Set<String> types = new LinkedHashSet<>();
        for (TrainingDetectedProblem p : problems) {
            types.add(p.getErrorType());
        }
        return TrainingFeedback.builder()
                .canProceed(false)
                .detectedProblems(problems)
                .errorTypes(new ArrayList<>(types))
                .revisionInstruction(revision)
                .build();
    }

    private static TrainingFeedback failWith(List<TrainingDetectedProblem> problems, String type, String revision) {
        List<TrainingDetectedProblem> p = trim(problems);
        TrainingDetectedProblem first = p.get(0);
        if (!type.equals(first.getErrorType())) {
            // keep requested primary type on first problem
        }
        return TrainingFeedback.builder()
                .canProceed(false)
                .detectedProblems(p)
                .errorTypes(List.of(type))
                .revisionInstruction(revision)
                .build();
    }

    private static TrainingDetectedProblem missingStep(String text, String hint) {
        return TrainingDetectedProblem.builder()
                .problemText(text)
                .errorType(TrainingErrorTypes.MISSING_STEP)
                .evidence("rule_slot")
                .fixHint(hint)
                .build();
    }

    private static TrainingDetectedProblem causalGap(String text, String hint) {
        return TrainingDetectedProblem.builder()
                .problemText(text)
                .errorType(TrainingErrorTypes.CAUSAL_GAP)
                .evidence("rule_chain")
                .fixHint(hint)
                .build();
    }

    private static TrainingDetectedProblem vague(String text, String hint) {
        return TrainingDetectedProblem.builder()
                .problemText(text)
                .errorType(TrainingErrorTypes.VAGUE_EXPRESSION)
                .evidence("rule_shallow")
                .fixHint(hint)
                .build();
    }

    private static TrainingDetectedProblem mechanismError(String text, String hint) {
        return TrainingDetectedProblem.builder()
                .problemText(text)
                .errorType(TrainingErrorTypes.MECHANISM_ERROR)
                .evidence("rule_mechanism")
                .fixHint(hint)
                .build();
    }
}
