package navigator.application.knowledge;

import navigator.domain.model.StructuredLearningGoal;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class KnowledgePackMetadata {

    private KnowledgePackMetadata() {}

    public record PackMeta(
            String knowledgeKey,
            String packId,
            String knowledgeType,
            String displayMode,
            List<String> phaseHighlights,
            List<String> commonMisconceptions,
            String scaffoldType,
            List<String> starterPrompts,
            String checkpointMode,
            String visualHintType
    ) {}

    public static PackMeta fromGoal(StructuredLearningGoal goal) {
        String corpus = corpus(goal);
        if (corpus.contains("os_process_thread") || (corpus.contains("进程") && corpus.contains("线程"))) {
            return comparePack("os_process_thread", "对比驾驶台", List.of("资源归属", "调度对象", "切换成本", "隔离性"), List.of("不要把线程=轻量进程当作全部"));
        }
        if (corpus.contains("net_tcp_handshake") || (corpus.contains("tcp") && corpus.contains("握手"))) {
            return sequencePack("net_tcp_handshake", "三步时序条", List.of("SYN", "SYN-ACK", "ACK"), List.of("不要只背“三次”"));
        }
        if (corpus.contains("ds_dfs_bfs") || ((corpus.contains("dfs") || corpus.contains("深度优先")) && (corpus.contains("bfs") || corpus.contains("广度优先")))) {
            return choicePack("ds_dfs_bfs", "算法选择地图", List.of("题型线索", "搜索顺序", "空间开销"), List.of("不要先背模板代码"));
        }
        if (corpus.contains("arch_cache_locality") || (corpus.contains("缓存") && corpus.contains("局部性"))) {
            return mechanismPack("arch_cache_locality", "访问模式对比卡", List.of("时间局部性", "空间局部性", "命中直觉"), List.of("先别下潜硬件细节"));
        }
        return null;
    }

    private static PackMeta comparePack(String id, String visual, List<String> highlights, List<String> misconceptions) {
        return new PackMeta(id, id, "COMPARE", "PACK", highlights, misconceptions, "COMPARE_BOARD", List.of("先做对比卡", "先说最易混点", "用场景判断"), "SCENARIO", visual);
    }
    private static PackMeta sequencePack(String id, String visual, List<String> highlights, List<String> misconceptions) {
        return new PackMeta(id, id, "SEQUENCE", "PACK", highlights, misconceptions, "SEQUENCE_BAR", List.of("先画时序", "解释为什么三次", "反事实推演"), "COUNTERFACTUAL", visual);
    }
    private static PackMeta choicePack(String id, String visual, List<String> highlights, List<String> misconceptions) {
        return new PackMeta(id, id, "CHOICE", "PACK", highlights, misconceptions, "CHOICE_MAP", List.of("先做选择树", "题型站队", "复盘误判"), "TYPE_MATCH", visual);
    }
    private static PackMeta mechanismPack(String id, String visual, List<String> highlights, List<String> misconceptions) {
        return new PackMeta(id, id, "MECHANISM", "PACK", highlights, misconceptions, "MECHANISM_CARD", List.of("先讲访问模式", "对比按行按列", "建立命中直觉"), "PERFORMANCE_JUDGE", visual);
    }

    private static String corpus(StructuredLearningGoal goal) {
        if (goal == null) return "";
        List<String> p = new ArrayList<>();
        add(p, goal.getRawGoalText());
        add(p, goal.getNormalizedGoalText());
        add(p, goal.getIntentDescription());
        add(p, goal.getSubject());
        add(p, goal.getSourceContext());
        add(p, goal.getPriorityModule());
        if (goal.getTopics() != null) p.addAll(goal.getTopics());
        return String.join("\n", p).toLowerCase(Locale.ROOT);
    }

    private static void add(List<String> parts, String value) {
        if (StringUtils.hasText(value)) parts.add(value);
    }
}
