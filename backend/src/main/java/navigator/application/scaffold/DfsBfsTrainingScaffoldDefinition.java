package navigator.application.scaffold;

import navigator.api.dto.scaffold.LearningActionCard;
import navigator.api.dto.scaffold.StageScaffold;

import java.util.List;

/**
 * DFS/BFS training stage cards.
 */
public final class DfsBfsTrainingScaffoldDefinition {

    public static final String STAGE_KEY = "TRAINING";

    public static final String ACTION_BFS_SHORTEST = "training_bfs_unweighted_shortest_path";
    public static final String ACTION_ORDER_CONSEQUENCE = "training_dfs_bfs_order_consequence";

    private DfsBfsTrainingScaffoldDefinition() {
    }

    public static List<String> orderedActionIds() {
        return List.of(ACTION_BFS_SHORTEST, ACTION_ORDER_CONSEQUENCE);
    }

    public static StageScaffold buildStage() {
        return StageScaffold.builder()
                .stageKey(STAGE_KEY)
                .stageTitle("表达内化")
                .stageGoal("把理解变成可用判断，写出方法选择和因果链。")
                .phaseGoal("我需要把理解变成可用判断，能解释为什么这题该用 BFS 或 DFS。")
                .stageDescription("每轮最多指出 1-2 个问题，只要求重写缺口，不提供标准答案。")
                .validatorType("TRAINING_RULES")
                .tutorMode("TRAINING_CONSTRAINED")
                .actionCards(List.of(
                        LearningActionCard.builder()
                                .actionId(ACTION_BFS_SHORTEST)
                                .title("为什么 BFS 常用于无权图最短路径")
                                .goal("要求用户写出『按层扩展 -> 更近先访问 -> 首次到达即最短』的因果链。")
                                .singleAction("先写结论，再写出『按层扩展 -> 更近先访问 -> 首次到达即最短』的因果链。")
                                .instructions("写 5-10 句，必须包含完整的因为 / 所以。")
                                .systemPrompt("先写结论，再写因果链。不要找标准答案，不要抄模板。必须写出『因为……所以……』的完整链条。")
                                .llmRole("训练纠错教练")
                                .userOutputLabel("本轮表达")
                                .allowedPrompts(List.of(
                                        "BFS 是怎么按层扩展的？",
                                        "为什么更近的点会更早被访问？",
                                        "首次到达终点说明了什么？"
                                ))
                                .forbiddenPrompts(List.of(
                                        "不要只给结论",
                                        "不要请求系统直接给标准题解",
                                        "不要写循环表述"
                                ))
                                .forbiddenActions(List.of(
                                        "禁止请求系统直接给标准题解",
                                        "禁止只给结论不写理由",
                                        "禁止『BFS 因为广度优先所以最短』这类循环表述"
                                ))
                                .passCriteria(List.of(
                                        "有明确结论",
                                        "有完整因果链",
                                        "点出首次到达和最短的关系"
                                ))
                                .completionCriteria(List.of(
                                        "明确写出方法选择结论",
                                        "至少包含一条完整因果链",
                                        "出现『按层扩展 -> 更近先访问 -> 首次到达即最短』"
                                ))
                                .nextActionHint("下一张：DFS / BFS 顺序差异会带来什么结果差异。")
                                .build(),
                        LearningActionCard.builder()
                                .actionId(ACTION_ORDER_CONSEQUENCE)
                                .title("DFS 和 BFS 的搜索顺序差异会带来什么结果差异")
                                .goal("要求用户写出『顺序不同 -> 路径 / 过程不同 -> 场景适配不同』。")
                                .singleAction("先写结论，再写出『顺序不同 -> 路径 / 过程不同 -> 场景适配不同』的因果链。")
                                .instructions("写 5-10 句，先比较顺序，再落到后果和场景。")
                                .systemPrompt("先写结论，再写因果链。不要只给标签，必须把顺序差异如何导致不同结果讲出来。")
                                .llmRole("训练纠错教练")
                                .userOutputLabel("本轮表达")
                                .allowedPrompts(List.of(
                                        "两种顺序分别先往哪走？",
                                        "顺序不同会带来什么过程差异？",
                                        "什么场景更适合其中一种？"
                                ))
                                .forbiddenPrompts(List.of(
                                        "不要只写『一个深度一个广度』",
                                        "不要复制前一阶段原话",
                                        "不要堆术语"
                                ))
                                .forbiddenActions(List.of(
                                        "禁止只给标签不写后果",
                                        "禁止复制前面 UNDERSTANDING 阶段原话直接交差",
                                        "禁止堆术语、堆字数冒充理解"
                                ))
                                .passCriteria(List.of(
                                        "比较顺序差异",
                                        "写出路径或过程差异",
                                        "落到场景适配"
                                ))
                                .completionCriteria(List.of(
                                        "至少包含一条完整因果链",
                                        "出现『顺序不同 -> 路径 / 过程不同 -> 场景适配不同』",
                                        "系统连续一轮不再检测到关键缺口"
                                ))
                                .nextActionHint("训练完成后进入反思沉淀。")
                                .build()
                ))
                .build();
    }
}
