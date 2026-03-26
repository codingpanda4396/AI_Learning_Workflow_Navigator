package navigator.application.scaffold;

import navigator.api.dto.scaffold.LearningActionCard;
import navigator.api.dto.scaffold.StageScaffold;

import java.util.List;

/**
 * DFS/BFS TRAINING：表达内化，两张训练卡。
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
                .stageGoal("用你自己的话把因果链说完整：先暴露问题，再在局部纠错中重构表达。")
                .stageDescription("系统每轮最多指出 1～2 个问题；你需要按重构要求改写，直到通过。")
                .validatorType("TRAINING_RULES")
                .tutorMode("TRAINING_CONSTRAINED")
                .actionCards(List.of(
                        LearningActionCard.builder()
                                .actionId(ACTION_BFS_SHORTEST)
                                .title("为什么 BFS 常用于无权图最短路径？")
                                .goal("说出「按层扩展 → 更近先被访问 → 第一次到达即最短」这条因果链，而不是只背「广度优先」。")
                                .instructions("写 5～10 句：强调层进、距离顺序、首次到达的意义；避免贴代码。")
                                .userOutputLabel("本轮表达")
                                .allowedPrompts(List.of(
                                        "BFS 是怎么一层层扩开的？",
                                        "为什么先看到更近的点？",
                                        "第一次碰到终点说明什么？"
                                ))
                                .forbiddenPrompts(List.of(
                                        "直接背定义",
                                        "贴完整代码",
                                        "让系统替你给标准答案"
                                ))
                                .passCriteria(List.of(
                                        "按层/层进直觉",
                                        "更近先被访问",
                                        "首次到达与最短的关系"
                                ))
                                .nextActionHint("下一张：对比 DFS 与 BFS 的顺序差异带来的结果差异。")
                                .build(),
                        LearningActionCard.builder()
                                .actionId(ACTION_ORDER_CONSEQUENCE)
                                .title("DFS 与 BFS 的搜索顺序差异，会导致什么结果差异？")
                                .goal("把「顺序不同 → 路径/过程不同 → 适用问题不同」说清楚，而不是只各说一个名词。")
                                .instructions("写 5～10 句：对比推进顺序，再落到结果特征与适用场景。")
                                .userOutputLabel("本轮表达")
                                .allowedPrompts(List.of(
                                        "两种顺序分别先往哪走？",
                                        "找到的第一条路径一定相同吗？",
                                        "什么时候更适合用其中一种？"
                                ))
                                .forbiddenPrompts(List.of(
                                        "只写「一个深度一个广度」",
                                        "贴模板代码"
                                ))
                                .passCriteria(List.of(
                                        "顺序/推进方式不同",
                                        "对路径或搜索过程的影响",
                                        "适用场景或问题类型差异"
                                ))
                                .nextActionHint("TRAINING 完成后进入反思收敛。")
                                .build()
                ))
                .build();
    }
}
