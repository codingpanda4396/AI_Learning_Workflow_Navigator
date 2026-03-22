package navigator.application.task;

import navigator.domain.enums.FeedbackStyle;
import navigator.domain.enums.ScaffoldIntensity;
import navigator.domain.enums.ScaffoldPromptIntent;
import navigator.domain.enums.TaskType;
import navigator.domain.model.CognitiveUnit;
import navigator.domain.model.LearnerStrategyProfile;
import navigator.domain.model.ScaffoldPrompt;
import navigator.domain.model.TaskBlueprint;
import navigator.domain.model.TaskScaffold;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 由 TaskBlueprint 生成执行期脚手架（规则生成，不调用 LLM）。
 */
public final class TaskScaffoldFactory {

    private TaskScaffoldFactory() {}

    public static TaskScaffold build(String sessionId, TaskBlueprint bp) {
        return build(sessionId, bp, null);
    }

    public static TaskScaffold build(String sessionId, TaskBlueprint bp, LearnerStrategyProfile strategyProfile) {
        ListBundle lists = buildListBundle(bp, strategyProfile);
        List<CognitiveUnit> units = buildCognitiveUnits(bp, lists);
        String taskLevel = taskLevelLine(bp.getTaskType(), lists.goal);

        return TaskScaffold.builder()
                .scaffoldId(TaskExecutionRuntime.newScaffoldId())
                .taskId(bp.getTaskId())
                .sessionId(sessionId)
                .taskType(bp.getTaskType() != null ? bp.getTaskType().name() : "CONCEPT_EXPLAIN")
                .taskLevelLearningIntent(taskLevel)
                .learningObjective(taskLevel)
                .whyThisTask(bp.getTaskMethod() != null ? bp.getTaskMethod() : "本任务是计划中的一环，完成后便于进入下一步。")
                .recommendedAskTemplates(lists.ask)
                .recommendedFollowupTemplates(lists.follow)
                .selfCheckTemplates(lists.selfCheck)
                .fallbackHints(lists.fallback)
                .completionSignals(lists.completion)
                .antiPatterns(lists.anti)
                .cognitiveUnits(units)
                .currentExecutionState("ORIENT")
                .suggestedExploreTurns(lists.suggestedExploreTurns)
                .suggestedCheckpointCount(lists.suggestedCheckpointCount)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * 读时补全：旧持久化 JSON 无 cognitiveUnits 时注入默认单元并刷新任务级认知文案。
     */
    public static void ensureCognitiveUnits(TaskScaffold scaffold, TaskBlueprint bp, LearnerStrategyProfile profile) {
        if (scaffold == null || bp == null) {
            return;
        }
        if (scaffold.getCognitiveUnits() != null && !scaffold.getCognitiveUnits().isEmpty()) {
            return;
        }
        ListBundle lists = buildListBundle(bp, profile);
        scaffold.setCognitiveUnits(buildCognitiveUnits(bp, lists));
        String taskLevel = taskLevelLine(bp.getTaskType(), lists.goal);
        scaffold.setTaskLevelLearningIntent(taskLevel);
        scaffold.setLearningObjective(taskLevel);
    }

    private static List<CognitiveUnit> buildCognitiveUnits(TaskBlueprint bp, ListBundle lists) {
        TaskType tt = bp.getTaskType() != null ? bp.getTaskType() : TaskType.CONCEPT_EXPLAIN;
        String g = truncate(lists.goal, 36);

        CognitiveUnit understand = CognitiveUnit.builder()
                .unitId("understand")
                .order(1)
                .label("搞清它怎么工作")
                .learningObjective(understandLearningObjective(tt, g))
                .targetOutcome("能用自己的话说清：这步在解决什么问题，关键对象或步骤是什么")
                .failureSignal("只能说标题或复述原文，说不清要解决什么或关键关系")
                .actionBullets(understandBullets(tt))
                .prompts(List.of())
                .build();

        List<ScaffoldPrompt> explorePrompts = new ArrayList<>();
        explorePrompts.add(ScaffoldPrompt.builder()
                .promptId("p_exp_primary")
                .prompt(lists.ask.get(0))
                .intent(ScaffoldPromptIntent.UNDERSTAND)
                .required(true)
                .build());
        if (lists.ask.size() > 1) {
            explorePrompts.add(ScaffoldPrompt.builder()
                    .promptId("p_exp_example")
                    .prompt(lists.ask.get(1))
                    .intent(ScaffoldPromptIntent.EXAMPLE)
                    .required(true)
                    .build());
        }
        explorePrompts.add(ScaffoldPrompt.builder()
                .promptId("p_exp_contrast")
                .prompt(lists.follow.get(0))
                .intent(ScaffoldPromptIntent.CONTRAST)
                .required(true)
                .build());
        if (lists.follow.size() > 1) {
            explorePrompts.add(ScaffoldPrompt.builder()
                    .promptId("p_exp_counter")
                    .prompt(lists.follow.get(1))
                    .intent(ScaffoldPromptIntent.COUNTEREXAMPLE)
                    .required(false)
                    .build());
        }

        CognitiveUnit explore = CognitiveUnit.builder()
                .unitId("explore")
                .order(2)
                .label("用例子跑一遍")
                .learningObjective("用一两个具体问题，把模糊点问成能讨论清楚的小问题")
                .targetOutcome("至少选一句下面的起步问法，并在对话里推进一小轮")
                .failureSignal("只泛泛说「不懂」，或直接要完整答案")
                .actionBullets(List.of(
                        "从下面起步问法里选一句发给导师",
                        "说说你已经试过什么、卡在哪一小步"
                ))
                .prompts(explorePrompts)
                .build();

        CognitiveUnit selfExplain = CognitiveUnit.builder()
                .unitId("self_explain")
                .order(3)
                .label("你自己讲清楚")
                .learningObjective("把刚才聊清楚的点，收成你自己的说法（别照抄原文）")
                .targetOutcome("用你自己的话写出关键点，长度达到最低要求即可")
                .failureSignal("复述原文、缺条件，或太笼统听不出你是否理解")
                .actionBullets(List.of(
                        "先一句话说结论，再补一句理由或最小例子",
                        "仍不确定的一点可以顺手写上（可选）"
                ))
                .prompts(List.of())
                .build();

        CognitiveUnit verify = CognitiveUnit.builder()
                .unitId("verify")
                .order(4)
                .label("快速练一下")
                .learningObjective("用一两句话自测：理解是不是站得住")
                .targetOutcome("读题后用自己的话作答，通过本轮小练习即可")
                .failureSignal("回答过短、偏题或与任务无关")
                .actionBullets(List.of("读题 → 想两个关键词 → 用一两句话串起来"))
                .prompts(List.of())
                .build();

        return List.of(understand, explore, selfExplain, verify);
    }

    private static String understandLearningObjective(TaskType tt, String goalSnippet) {
        return switch (tt) {
            case MICRO_PRACTICE, GUIDED_EXAMPLE ->
                    "在「" + goalSnippet + "」上先弄清操作/步骤结构，再进入练习对话";
            case CHECKPOINT_REVIEW ->
                    "回顾「" + goalSnippet + "」的检查点：先对齐标准，再定位薄弱处";
            case COMPARE_AND_CONNECT ->
                    "为「" + goalSnippet + "」建立对比框架：各自解决什么、边界在哪里";
            case SELF_EXPLANATION, CONCEPT_EXPLAIN ->
                    "用「" + goalSnippet + "」的一个最小例子，看清结构和容易混的点";
        };
    }

    private static List<String> understandBullets(TaskType tt) {
        return switch (tt) {
            case MICRO_PRACTICE, GUIDED_EXAMPLE -> List.of(
                    "你要完成的具体动作或产出是什么",
                    "中间最关键的一步是什么",
                    "最容易出错的前置条件是什么"
            );
            case COMPARE_AND_CONNECT -> List.of(
                    "A 与 B 各自解决什么问题",
                    "它们的输入输出或适用场景差在哪里",
                    "什么时候该用哪一个"
            );
            default -> List.of(
                    "最小例子长什么样（一句话）",
                    "它主要解决什么问题",
                    "和最容易混淆的说法差在哪一句"
            );
        };
    }

    private static String taskLevelLine(TaskType tt, String goal) {
        String g = truncate(goal, 32);
        String core = switch (tt != null ? tt : TaskType.CONCEPT_EXPLAIN) {
            case GUIDED_EXAMPLE, MICRO_PRACTICE -> "在「" + g + "」上做小步练习，做出可见的小产出";
            case CHECKPOINT_REVIEW -> "把「" + g + "」相关的检查点再过一遍，找出薄弱处";
            case COMPARE_AND_CONNECT -> "把「" + g + "」放进对比框架里，搞清边界和用法";
            case SELF_EXPLANATION, CONCEPT_EXPLAIN -> "用最小例子搞懂「" + g + "」";
        };
        return "这一步我们一起搞懂：" + core;
    }

    private static ListBundle buildListBundle(TaskBlueprint bp, LearnerStrategyProfile strategyProfile) {
        String goal = bp.getGoal() != null ? bp.getGoal() : bp.getTitle();
        String prompt = bp.getRecommendedPromptTemplate() != null
                ? bp.getRecommendedPromptTemplate()
                : (bp.getPromptScaffold() != null ? bp.getPromptScaffold() : "请围绕任务目标向导师提问。");
        List<String> ask = new ArrayList<>();
        ask.add(prompt);
        ask.add("请用最小例子说明：" + truncate(goal, 40));
        List<String> follow = new ArrayList<>();
        follow.add("和上一概念相比，最大的差异是什么？");
        follow.add("如果去掉其中一个条件，结论还成立吗？");
        List<String> selfCheck = new ArrayList<>();
        if (bp.getSelfEvaluationQuestions() != null) {
            selfCheck.addAll(bp.getSelfEvaluationQuestions());
        }
        selfCheck.add("我这样理解对吗：" + truncate(goal, 30) + "……");
        List<String> fallback = new ArrayList<>();
        if (bp.getFallbackAction() != null) {
            fallback.add(bp.getFallbackAction());
        }
        fallback.add("先只问一个更小的子问题，再逐步扩展。");
        fallback.add("用自己的话复述当前卡住的点，再请导师针对该点解释。");
        List<String> completion = new ArrayList<>();
        if (bp.getCompletionCriteria() != null) {
            completion.addAll(bp.getCompletionCriteria());
        }
        if (completion.isEmpty()) {
            completion.add("能独立复述本任务的核心要点");
        }
        List<String> anti = new ArrayList<>(List.of(
                "不要让导师一次性讲完整章",
                "不要只复制定义而不举例",
                "不要跳过自我复述直接要答案"
        ));
        if (strategyProfile != null && strategyProfile.getFeedbackStyle() == FeedbackStyle.DIRECT) {
            anti.add("不要期待导师反复追问，可主动精简提问");
        }

        int suggestedExploreTurns = 2;
        int suggestedCheckpointCount = 1;
        if (strategyProfile != null && strategyProfile.getScaffoldIntensity() != null) {
            ScaffoldIntensity intensity = strategyProfile.getScaffoldIntensity();
            if (intensity == ScaffoldIntensity.LIGHT) {
                suggestedExploreTurns = 1;
                suggestedCheckpointCount = 1;
            } else if (intensity == ScaffoldIntensity.STRICT) {
                suggestedExploreTurns = 3;
                suggestedCheckpointCount = 2;
            }
        }

        return new ListBundle(goal, prompt, ask, follow, selfCheck, fallback, completion, anti,
                suggestedExploreTurns, suggestedCheckpointCount);
    }

    private static String truncate(String s, int max) {
        if (s == null) {
            return "";
        }
        return s.length() <= max ? s : s.substring(0, max) + "…";
    }

    private record ListBundle(
            String goal,
            String primaryPrompt,
            List<String> ask,
            List<String> follow,
            List<String> selfCheck,
            List<String> fallback,
            List<String> completion,
            List<String> anti,
            int suggestedExploreTurns,
            int suggestedCheckpointCount
    ) {}
}
