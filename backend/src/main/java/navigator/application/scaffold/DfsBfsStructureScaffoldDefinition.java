package navigator.application.scaffold;

import navigator.api.dto.scaffold.LearningActionCard;
import navigator.api.dto.scaffold.StageScaffold;

import java.util.List;

/**
 * DFS/BFS 知识点 STRUCTURE 阶段动作卡（规则生成，不调用 LLM）。
 */
public final class DfsBfsStructureScaffoldDefinition {

    public static final String ACTION_PROBLEM = "dfs_bfs_structure_problem";
    public static final String ACTION_POSITION = "dfs_bfs_structure_position";
    public static final String ACTION_DIFF = "dfs_bfs_structure_diff";

    private DfsBfsStructureScaffoldDefinition() {
    }

    public static List<String> orderedActionIds() {
        return List.of(ACTION_PROBLEM, ACTION_POSITION, ACTION_DIFF);
    }

    public static StageScaffold buildStage() {
        return StageScaffold.builder()
                .stageKey(DfsBfsStructureValidator.STAGE_KEY)
                .stageTitle("结构建立")
                .stageGoal("先搭清 DFS/BFS 在图搜索里的角色，不进入实现与题型。")
                .stageDescription("本阶段只回答：解决什么问题、在知识地图里站哪、和相邻概念差在哪。")
                .validatorType("STRUCTURE_RULES")
                .tutorMode("CONSTRAINED_TEMPLATE")
                .actionCards(List.of(
                        LearningActionCard.builder()
                                .actionId(ACTION_PROBLEM)
                                .title("DFS/BFS 在解决什么问题")
                                .goal("用一句话说清「这类方法帮什么忙」，不要讲算法步骤。")
                                .instructions("写 2～4 句：面向的场景/要达成的目标（例如连通、可达、分层等），避免细节。")
                                .userOutputLabel("我的回答")
                                .allowedPrompts(List.of(
                                        "它主要解决哪类「在图里走路」的问题？",
                                        "没有它时，你会卡在哪类任务上？"
                                ))
                                .forbiddenPrompts(List.of(
                                        "写代码或伪代码",
                                        "谈复杂度、栈、队列、递归实现",
                                        "具体题型或刷题路径"
                                ))
                                .passCriteria(List.of(
                                        "说清楚「用途/场景」而非步骤",
                                        "不出现实现与复杂度关键词"
                                ))
                                .nextActionHint("下一张：在知识体系里它站哪。")
                                .build(),
                        LearningActionCard.builder()
                                .actionId(ACTION_POSITION)
                                .title("DFS/BFS 在知识体系中的位置")
                                .goal("说明它上游/下游各是什么，不要展开机制。")
                                .instructions("写 2～4 句：它通常接在「图表示」之后、服务哪些后续主题（如遍历应用），保持粗粒度。")
                                .userOutputLabel("我的回答")
                                .allowedPrompts(List.of(
                                        "它和「表示图」的关系是什么？",
                                        "它更偏基础工具还是偏上层应用？"
                                ))
                                .forbiddenPrompts(List.of(
                                        "展开队列如何工作",
                                        "最短路算法细节"
                                ))
                                .passCriteria(List.of(
                                        "能指出前后邻接概念",
                                        "不讲过程与实现"
                                ))
                                .nextActionHint("下一张：两者核心差异。")
                                .build(),
                        LearningActionCard.builder()
                                .actionId(ACTION_DIFF)
                                .title("DFS/BFS 的核心差异是什么")
                                .goal("用对比句说清「先扩展谁/搜索形态」，不要讲代码。")
                                .instructions("写 2～4 句：对比「探索顺序/形态」层面的差异，避免机制长文。")
                                .userOutputLabel("我的回答")
                                .allowedPrompts(List.of(
                                        "哪一个更「一路走深」？哪一个更「一圈圈扩」？",
                                        "你用什么线索区分它们？"
                                ))
                                .forbiddenPrompts(List.of(
                                        "贴代码",
                                        "复杂度推导",
                                        "具体题目怎么做"
                                ))
                                .passCriteria(List.of(
                                        "差异落在「搜索形态/顺序直觉」",
                                        "不出现实现细节"
                                ))
                                .nextActionHint("完成 STRUCTURE，可进入下一阶段。")
                                .build()
                ))
                .build();
    }
}
