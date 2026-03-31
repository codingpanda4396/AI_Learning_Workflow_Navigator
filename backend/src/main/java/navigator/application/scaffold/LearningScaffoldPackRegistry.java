package navigator.application.scaffold;

import java.util.Set;

/**
 * 启用学习脚手架引擎的知识点包（增量扩展，避免散落在各处的硬编码字符串）。
 */
public final class LearningScaffoldPackRegistry {

    public static final String DFS_BFS = "ds_dfs_bfs";

    private static final Set<String> ENGINE_ENABLED = Set.of(DFS_BFS);

    private LearningScaffoldPackRegistry() {
    }

    public static boolean supportsLearningScaffoldEngine(String packId) {
        return packId != null && ENGINE_ENABLED.contains(packId);
    }
}
