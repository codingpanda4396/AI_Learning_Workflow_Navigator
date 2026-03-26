package navigator.application.scaffold;

import navigator.api.dto.scaffold.LearningActionCard;
import navigator.api.dto.scaffold.StageScaffold;

import java.util.List;

/**
 * DFS/BFS 知识点 STRUCTURE 阶段：四张脚手架卡（多卡可选，进度由 {@link LearningScaffoldEngineState} 记录）。
 */
public final class DfsBfsStructureScaffoldDefinition {

    public static final String ACTION_POSITION = "dfs_bfs_structure_position";
    public static final String ACTION_PREREQ = "dfs_bfs_structure_prereq";
    public static final String ACTION_NEXT = "dfs_bfs_structure_next";
    public static final String ACTION_DEFER = "dfs_bfs_structure_defer";

    /** @deprecated 兼容旧会话 JSON */
    @Deprecated
    public static final String ACTION_PROBLEM = "dfs_bfs_structure_problem";
    /** @deprecated 兼容旧会话 JSON */
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
                .stageGoal("先搭清 DFS/BFS 在知识结构中的位置与边界，不进入实现与题型。")
                .stageDescription("点选左侧脚手架，系统先为你搭骨架；你只需轻量反馈，不必长段作答。")
                .validatorType("STRUCTURE_SCAFFOLD")
                .tutorMode("LLM_SKELETON")
                .actionCards(List.of(
                        LearningActionCard.builder()
                                .actionId(ACTION_POSITION)
                                .title("DFS / BFS 在知识体系中的位置")
                                .goal("看清它属于哪条主线、站在哪一层。")
                                .instructions("点击「点击学习」生成骨架；不讲代码细节。")
                                .userOutputLabel("骨架")
                                .allowedPrompts(List.of())
                                .forbiddenPrompts(List.of())
                                .passCriteria(List.of())
                                .nextActionHint("可选：前置概念或后续连接。")
                                .build(),
                        LearningActionCard.builder()
                                .actionId(ACTION_PREREQ)
                                .title("学它之前要先知道什么")
                                .goal("列出最小前置概念，避免跳步。")
                                .instructions("点击卡片生成骨架。")
                                .userOutputLabel("骨架")
                                .allowedPrompts(List.of())
                                .forbiddenPrompts(List.of())
                                .passCriteria(List.of())
                                .nextActionHint("可选：后续会接到哪些主题。")
                                .build(),
                        LearningActionCard.builder()
                                .actionId(ACTION_NEXT)
                                .title("学完它会连接到哪些主题")
                                .goal("知道学完后自然往哪走。")
                                .instructions("点击卡片生成骨架。")
                                .userOutputLabel("骨架")
                                .allowedPrompts(List.of())
                                .forbiddenPrompts(List.of())
                                .passCriteria(List.of())
                                .nextActionHint("可选：本轮先不碰哪些细节。")
                                .build(),
                        LearningActionCard.builder()
                                .actionId(ACTION_DEFER)
                                .title("这一轮先不要进入哪些细节")
                                .goal("划边界，避免被实现细节淹没。")
                                .instructions("点击卡片生成骨架。")
                                .userOutputLabel("骨架")
                                .allowedPrompts(List.of())
                                .forbiddenPrompts(List.of())
                                .passCriteria(List.of())
                                .nextActionHint("条件满足后可进入机制理解。")
                                .build()
                ))
                .build();
    }
}
