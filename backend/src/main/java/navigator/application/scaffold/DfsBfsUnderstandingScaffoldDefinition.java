package navigator.application.scaffold;

import navigator.api.dto.scaffold.LearningActionCard;
import navigator.api.dto.scaffold.StageScaffold;

import java.util.List;

/**
 * DFS/BFS 知识点 UNDERSTANDING 阶段：机制叙述（规则生成，不调用 LLM）。
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
                .stageGoal("用你自己的话说明推进过程：DFS 如何一路深入与回退，BFS 如何按层展开。")
                .stageDescription("本阶段谈「如何推进」，仍避免贴代码与复杂度推导；要说清起点、扩展方式与顺序意义。")
                .validatorType("UNDERSTANDING_RULES")
                .tutorMode("CONSTRAINED_TEMPLATE")
                .actionCards(List.of(
                        LearningActionCard.builder()
                                .actionId(ACTION_DFS_STEPS)
                                .title("DFS 是怎么一步步向前探索的？")
                                .goal("说明从哪里开始、下一步怎么选、什么时候往回退，而不是只复述「深度优先」。")
                                .instructions("写 4～8 句：可结合「未访问邻居 / 走到底 / 无路可走再退回」这类叙述，避免伪代码。")
                                .userOutputLabel("我的机制叙述")
                                .allowedPrompts(List.of(
                                        "我从哪个起点出发？",
                                        "走到死胡同时我怎么做？",
                                        "下一步通常从哪些候选里选？"
                                ))
                                .forbiddenPrompts(List.of(
                                        "贴完整代码或大段模板",
                                        "展开时间复杂度证明",
                                        "只下定义不说推进过程"
                                ))
                                .passCriteria(List.of(
                                        "提到起点或出发方式",
                                        "描述向前/扩展的一步怎么走",
                                        "描述无路时的回退或回溯"
                                ))
                                .nextActionHint("下一张：说明 BFS 为何按层推进。")
                                .build(),
                        LearningActionCard.builder()
                                .actionId(ACTION_BFS_LAYERS)
                                .title("BFS 为什么天然按层推进？")
                                .goal("说明一层层展开的顺序直觉，以及这种顺序带来的意义（例如离起点更近先被看到）。")
                                .instructions("写 4～8 句：强调「同层 / 下一层」的扩展顺序，不要写成队列实现课。")
                                .userOutputLabel("我的机制叙述")
                                .allowedPrompts(List.of(
                                        "先处理哪一批顶点？",
                                        "为什么像水波纹一圈圈扩散？",
                                        "这种顺序和「距离」直觉有什么关系？"
                                ))
                                .forbiddenPrompts(List.of(
                                        "贴代码实现",
                                        "大讲最短路算法族谱",
                                        "只说一句广度优先了事"
                                ))
                                .passCriteria(List.of(
                                        "提到按层或一圈圈扩展",
                                        "解释同层与下一层的关系",
                                        "点出顺序带来的意义（如更近先访问）"
                                ))
                                .nextActionHint("UNDERSTANDING 完成，可进入探索对话。")
                                .build()
                ))
                .build();
    }
}
