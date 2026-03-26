package navigator.application.scaffold;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 轻量中文槽位与浅层理解检测（无 LLM）。
 */
public final class PseudoUnderstandingDetector {

    private static final Pattern DFS_DEFINITION_ONLY = Pattern.compile(
            "(深度优先|DFS\\b|深度优先搜索)");
    private static final Pattern BFS_DEFINITION_ONLY = Pattern.compile(
            "(广度优先|BFS\\b|广度优先搜索)");

    private static final Pattern[] DFS_START = {
            Pattern.compile("起点|起始|从.*开始|出发|任选.*节点|选定|初始|源点"),
            Pattern.compile("从.{0,6}(顶点|节点|根)"),
    };
    private static final Pattern[] DFS_NEXT = {
            Pattern.compile("邻接|邻居|下一(步|个)|扩展|走向|沿着|未访问|选.*(条|个)|继续走|走到"),
            Pattern.compile("优先.*(深入|往下|向深)"),
    };
    private static final Pattern[] DFS_BACK = {
            Pattern.compile("回退|回溯|无路|退回|返回|撤销|走不通|不能再走|死胡同|退栈"),
    };

    private static final Pattern[] BFS_START = {
            Pattern.compile("起点|起始|从.*开始|出发|源"),
    };
    private static final Pattern[] BFS_LAYER = {
            Pattern.compile("层|同层|逐层|一圈|一圈圈|每一层|按层|一层层|水波|波纹"),
            Pattern.compile("先.*(处理|访问).*(再|然后).*(下一层|下一圈)"),
    };
    private static final Pattern[] BFS_ORDER = {
            Pattern.compile("最短|距离|近|先近|先.*后|顺序|意义|更近|先被"),
    };

    private PseudoUnderstandingDetector() {
    }

    public static DetectionResult detectDfs(String raw) {
        String text = raw == null ? "" : raw.trim();
        String compact = text.replaceAll("\\s+", "");
        String norm = text.toLowerCase(Locale.ROOT);

        Set<String> matched = new LinkedHashSet<>();
        matchGroup(DFS_START, text, compact, matched, "起点或出发方式");
        matchGroup(DFS_NEXT, text, compact, matched, "下一步如何扩展");
        matchGroup(DFS_BACK, text, compact, matched, "无路可走时的回退");

        List<String> missing = new ArrayList<>();
        if (!anyMatch(DFS_START, text, compact)) {
            missing.add("起点或出发方式");
        }
        if (!anyMatch(DFS_NEXT, text, compact)) {
            missing.add("下一步如何扩展");
        }
        if (!anyMatch(DFS_BACK, text, compact)) {
            missing.add("无路可走时的回退");
        }

        boolean shallow = isShallowDfs(text, compact, norm, matched.size());
        return new DetectionResult(matched, missing, shallow);
    }

    public static DetectionResult detectBfs(String raw) {
        String text = raw == null ? "" : raw.trim();
        String compact = text.replaceAll("\\s+", "");
        String norm = text.toLowerCase(Locale.ROOT);

        Set<String> matched = new LinkedHashSet<>();
        matchGroup(BFS_START, text, compact, matched, "起点或源");
        matchGroup(BFS_LAYER, text, compact, matched, "按层或一圈圈扩展");
        matchGroup(BFS_ORDER, text, compact, matched, "顺序意义或距离直觉");

        List<String> missing = new ArrayList<>();
        if (!anyMatch(BFS_START, text, compact)) {
            missing.add("起点或出发");
        }
        if (!anyMatch(BFS_LAYER, text, compact)) {
            missing.add("按层或同层扩展");
        }
        if (!anyMatch(BFS_ORDER, text, compact)) {
            missing.add("顺序意义（如更近先访问）");
        }

        boolean shallow = isShallowBfs(text, compact, norm, matched.size());
        return new DetectionResult(matched, missing, shallow);
    }

    private static boolean isShallowDfs(String text, String compact, String norm, int slotMatches) {
        boolean nameEcho = DFS_DEFINITION_ONLY.matcher(compact).find() || norm.contains("dfs");
        boolean longEnough = text.length() >= 90;
        if (longEnough && slotMatches >= 2) {
            return false;
        }
        if (nameEcho && text.length() < 100 && slotMatches < 2) {
            return true;
        }
        return text.length() < 45 && slotMatches < 2;
    }

    private static boolean isShallowBfs(String text, String compact, String norm, int slotMatches) {
        boolean nameEcho = BFS_DEFINITION_ONLY.matcher(compact).find() || norm.contains("bfs");
        boolean longEnough = text.length() >= 90;
        if (longEnough && slotMatches >= 2) {
            return false;
        }
        if (nameEcho && text.length() < 100 && slotMatches < 2) {
            return true;
        }
        return text.length() < 45 && slotMatches < 2;
    }

    private static void matchGroup(Pattern[] patterns, String text, String compact, Set<String> out, String label) {
        if (anyMatch(patterns, text, compact)) {
            out.add(label);
        }
    }

    private static boolean anyMatch(Pattern[] patterns, String text, String compact) {
        for (Pattern p : patterns) {
            if (p.matcher(text).find() || p.matcher(compact).find()) {
                return true;
            }
        }
        return false;
    }

    public record DetectionResult(List<String> matchedLabels, List<String> missingSlots, boolean shallowUnderstanding) {

        public DetectionResult(Set<String> matchedSet, List<String> missingSlots, boolean shallowUnderstanding) {
            this(new ArrayList<>(matchedSet), missingSlots, shallowUnderstanding);
        }
    }
}
