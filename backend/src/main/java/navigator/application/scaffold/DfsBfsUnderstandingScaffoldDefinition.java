package navigator.application.scaffold;

import navigator.api.dto.scaffold.LearningActionCard;
import navigator.api.dto.scaffold.StageScaffold;

import java.util.List;

/**
 * DFS/BFS understanding stage cards.
 */
public final class DfsBfsUnderstandingScaffoldDefinition {

    public static final String STAGE_KEY = "UNDERSTANDING";

    public static final String ACTION_DFS_STEPS = "dfs_bfs_understanding_dfs_steps";
    public static final String ACTION_BFS_LAYERS = "dfs_bfs_understanding_bfs_layers";

    private DfsBfsUnderstandingScaffoldDefinition() {
    }

    public static List<String> orderedActionIds() {
        return List.of(ACTION_DFS_STEPS, ACTION_BFS_LAYERS);
    }

    public static StageScaffold buildStage() {
        return StageScaffold.builder()
                .stageKey(STAGE_KEY)
                .stageTitle("机制理解")
                .stageGoal("用自己的话说清 DFS 怎么推进和回退，BFS 为什么按层展开。")
                .phaseGoal("我需要用自己的话说清楚 DFS 怎么推进和回退，BFS 为什么按层展开。")
                .stageDescription("这一阶段只讲推进机制，不讲代码、不讲复杂度、不讲题解。")
                .validatorType("UNDERSTANDING_RULES")
                .tutorMode("CONSTRAINED_TEMPLATE")
                .actionCards(List.of(
                        LearningActionCard.builder()
                                .actionId(ACTION_DFS_STEPS)
                                .title("DFS：从哪里开始，怎么往前走，什么时候回退")
                                .goal("用户必须把 DFS 的推进链讲完整，而不是只背定义。")
                                .singleAction("按『起点 -> 向前探索 -> 无路可走再回退』的模板，自述一次 DFS 推进过程。")
                                .instructions("写 4-6 句，重点是过程，不是标签。")
                                .systemPrompt("不要下定义，不要只写『DFS 是深度优先』。请按『起点 -> 扩展 -> 回退』把过程讲出来。")
                                .llmRole("伪理解探测器")
                                .userOutputLabel("DFS 机制叙述")
                                .allowedPrompts(List.of(
                                        "从哪里开始？",
                                        "下一步怎么选未访问的点？",
                                        "什么时候需要回退？"
                                ))
                                .forbiddenPrompts(List.of(
                                        "不要只写定义标签",
                                        "不要粘贴代码",
                                        "不要讲复杂度"
                                ))
                                .forbiddenActions(List.of(
                                        "禁止只写定义标签",
                                        "禁止粘贴代码",
                                        "禁止讲复杂度"
                                ))
                                .passCriteria(List.of(
                                        "出现起点",
                                        "出现向前探索",
                                        "出现无路可走再回退"
                                ))
                                .completionCriteria(List.of(
                                        "DFS 表述里出现『向前探索』和『无路可走再回退』",
                                        "文本不是定义复述，而是推进叙述"
                                ))
                                .nextActionHint("下一张：BFS 为什么天然按层展开。")
                                .build(),
                        LearningActionCard.builder()
                                .actionId(ACTION_BFS_LAYERS)
                                .title("BFS：先处理谁，下一层怎么展开，这种顺序意味着什么")
                                .goal("用户必须把 BFS 的层次推进和顺序意义讲完整。")
                                .singleAction("按『同层先处理 -> 再到下一层 -> 这种顺序意味着什么』的模板，自述一次 BFS 推进过程。")
                                .instructions("写 4-6 句，重点是按层推进带来的意义。")
                                .systemPrompt("不要只写『BFS 是广度优先』。请按『同层 -> 下一层 -> 顺序意义』把过程讲出来。")
                                .llmRole("伪理解探测器")
                                .userOutputLabel("BFS 机制叙述")
                                .allowedPrompts(List.of(
                                        "先处理谁？",
                                        "下一层是怎么展开的？",
                                        "这种顺序意味着什么？"
                                ))
                                .forbiddenPrompts(List.of(
                                        "不要只背最短路结论",
                                        "不要粘贴代码",
                                        "不要讲复杂度"
                                ))
                                .forbiddenActions(List.of(
                                        "禁止把 BFS 最短路当结论直接背出而不解释原因",
                                        "禁止只写定义标签",
                                        "禁止粘贴代码"
                                ))
                                .passCriteria(List.of(
                                        "出现同层先处理",
                                        "出现再到下一层",
                                        "出现顺序意义"
                                ))
                                .completionCriteria(List.of(
                                        "BFS 表述里出现『同层先处理，再到下一层』",
                                        "明确说出 BFS 顺序的意义，例如更近的点更早被访问",
                                        "全文无代码、无复杂度讨论"
                                ))
                                .nextActionHint("理解通过后进入训练。")
                                .build()
                ))
                .build();
    }
}
