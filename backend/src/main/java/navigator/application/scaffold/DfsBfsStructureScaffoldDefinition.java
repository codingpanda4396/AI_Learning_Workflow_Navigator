package navigator.application.scaffold;

import navigator.api.dto.scaffold.LearningActionCard;
import navigator.api.dto.scaffold.StageScaffold;
import navigator.domain.model.LearningScaffoldEngineState;

import java.util.List;

/**
 * DFS/BFS structure stage cards.
 */
public final class DfsBfsStructureScaffoldDefinition {

    public static final String ACTION_POSITION = "dfs_bfs_structure_position";
    public static final String ACTION_PREREQ = "dfs_bfs_structure_prereq";
    public static final String ACTION_NEXT = "dfs_bfs_structure_next";
    public static final String ACTION_DEFER = "dfs_bfs_structure_defer";

    /** @deprecated compatibility for legacy runtime JSON */
    @Deprecated
    public static final String ACTION_PROBLEM = "dfs_bfs_structure_problem";
    /** @deprecated compatibility for legacy runtime JSON */
    @Deprecated
    public static final String ACTION_DIFF = "dfs_bfs_structure_diff";

    private DfsBfsStructureScaffoldDefinition() {
    }

    public static List<String> orderedActionIds() {
        return List.of(ACTION_POSITION, ACTION_PREREQ, ACTION_NEXT, ACTION_DEFER);
    }

    public static boolean isValidPromptKey(String key) {
        if (key == null || key.isBlank()) {
            return false;
        }
        return orderedActionIds().contains(key);
    }

    public static StageScaffold buildStage() {
        return StageScaffold.builder()
                .stageKey(DfsBfsStructureValidator.STAGE_KEY)
                .stageTitle("结构建立")
                .stageGoal("先把 DFS / BFS 放回知识地图里，建立位置、前置、后续和边界。")
                .phaseGoal("先把 DFS / BFS 放回知识地图里，知道它们解决什么、和什么相邻、这轮先不碰什么。")
                .stageDescription("这一阶段只做结构动作，不讲实现、不讲复杂度、不讲题解。")
                .validatorType("STRUCTURE_SCAFFOLD")
                .tutorMode("LLM_SKELETON")
                .actionCards(List.of(
                        LearningActionCard.builder()
                                .actionId(ACTION_POSITION)
                                .title("它属于什么主题")
                                .goal("先写出 DFS / BFS 在图或树搜索中的位置。")
                                .singleAction("用一句话写出 DFS / BFS 属于哪一层知识主题。")
                                .instructions("只写位置，不写实现，不写复杂度。")
                                .systemPrompt("不要讲代码、复杂度、刷题模板。只写它在知识地图里的位置。")
                                .llmRole("结构校准器")
                                .userOutputLabel("位置句")
                                .allowedPrompts(List.of(
                                        "DFS / BFS 属于哪一类搜索或遍历方法？",
                                        "它们在图和树这条线上处于什么位置？"
                                ))
                                .forbiddenPrompts(List.of(
                                        "不要写代码或伪代码",
                                        "不要讲时间 / 空间复杂度",
                                        "不要直接展开最短路证明"
                                ))
                                .forbiddenActions(List.of(
                                        "禁止写代码、伪代码、队列栈递归实现",
                                        "禁止讨论时间 / 空间复杂度",
                                        "禁止直接进入 BFS 为何最短路"
                                ))
                                .passCriteria(List.of(
                                        "明确写出 DFS / BFS 属于图或树搜索 / 遍历这一层",
                                        "整句停留在结构定位，不越界到机制或题解"
                                ))
                                .completionCriteria(List.of(
                                        "至少明确一次 DFS / BFS 属于图 / 树搜索或遍历",
                                        "全文未出现实现、复杂度、题解内容"
                                ))
                                .nextActionHint("下一张：学它之前要先知道什么。")
                                .build(),
                        LearningActionCard.builder()
                                .actionId(ACTION_PREREQ)
                                .title("学它前要先知道什么")
                                .goal("只列最小前置，不扩写实现。")
                                .singleAction("用一句话写出学 DFS / BFS 前必须先站稳的前置概念。")
                                .instructions("前置只写最小必要概念，不写题型套路。")
                                .systemPrompt("只填前置概念，不要跳到具体题解和实现技巧。")
                                .llmRole("结构校准器")
                                .userOutputLabel("前置句")
                                .allowedPrompts(List.of(
                                        "学 DFS / BFS 前至少要知道什么？",
                                        "如果前置没站稳，会卡在哪？"
                                ))
                                .forbiddenPrompts(List.of(
                                        "不要开始讲 BFS 最短路因果链",
                                        "不要把前置写成代码模板"
                                ))
                                .forbiddenActions(List.of(
                                        "禁止用做题经验代替前置概念",
                                        "禁止把前置写成实现步骤"
                                ))
                                .passCriteria(List.of(
                                        "至少写出一个前置概念",
                                        "前置内容仍停留在知识结构层"
                                ))
                                .completionCriteria(List.of(
                                        "至少写出一个前置概念",
                                        "全文未出现实现、复杂度、题解内容"
                                ))
                                .nextActionHint("下一张：学完它会连到哪里。")
                                .build(),
                        LearningActionCard.builder()
                                .actionId(ACTION_NEXT)
                                .title("学完会连到什么")
                                .goal("让用户知道学完自然会走向哪些问题。")
                                .singleAction("用一句话写出学完 DFS / BFS 后会连到哪些主题或问题。")
                                .instructions("只写后续连接，不把后续内容讲完。")
                                .systemPrompt("写『学完会连到哪里』，不要在这里把后面几阶段都讲完。")
                                .llmRole("结构校准器")
                                .userOutputLabel("后续句")
                                .allowedPrompts(List.of(
                                        "学完 DFS / BFS 后通常会接到哪些问题？",
                                        "它会把你带向哪些相邻主题？"
                                ))
                                .forbiddenPrompts(List.of(
                                        "不要直接展开具体题解",
                                        "不要把后续主题写成长段机制说明"
                                ))
                                .forbiddenActions(List.of(
                                        "禁止围绕某道题展开题解",
                                        "禁止在后续连接卡里抢跑后面阶段内容"
                                ))
                                .passCriteria(List.of(
                                        "至少写出一个后续连接",
                                        "用户能看懂学完之后自然往哪走"
                                ))
                                .completionCriteria(List.of(
                                        "至少写出一个后续连接",
                                        "仍未越界到机制和题解"
                                ))
                                .nextActionHint("最后一张：这轮先不展开什么。")
                                .build(),
                        LearningActionCard.builder()
                                .actionId(ACTION_DEFER)
                                .title("这轮先不展开什么")
                                .goal("明确边界，避免被实现和题型细节淹没。")
                                .singleAction("用一句话写出这轮先不展开的一类细节。")
                                .instructions("只划边界，不展开解释。")
                                .systemPrompt("明确『这轮先不做什么』，只画边界，不补充长解释。")
                                .llmRole("结构校准器")
                                .userOutputLabel("边界句")
                                .allowedPrompts(List.of(
                                        "这轮先不展开哪些实现细节？",
                                        "哪些内容应该留到后面阶段再讲？"
                                ))
                                .forbiddenPrompts(List.of(
                                        "不要贴代码、不要讲复杂度、不要写题解",
                                        "不要在边界卡里继续讲机制"
                                ))
                                .forbiddenActions(List.of(
                                        "禁止刷题场景展开",
                                        "禁止长段解释机制",
                                        "禁止把边界卡写成另一张理解卡"
                                ))
                                .passCriteria(List.of(
                                        "至少写出一个『这轮不展开』的边界",
                                        "四张卡合起来形成位置 / 前置 / 后续 / 边界"
                                ))
                                .completionCriteria(List.of(
                                        "至少写出一个『这轮不展开』的边界",
                                        "四格都被填满"
                                ))
                                .nextActionHint("结构完成后进入机制理解。")
                                .build()
                ))
                .build();
    }
}
