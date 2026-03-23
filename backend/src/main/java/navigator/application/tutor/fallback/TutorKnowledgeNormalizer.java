package navigator.application.tutor.fallback;

/**
 * 将前端展示名 / 英文 code 归一为缓存与兜底使用的 canonical key。
 */
public final class TutorKnowledgeNormalizer {

    public static final String UNKNOWN = "unknown";
    public static final String BINARY_TREE = "binary_tree";
    public static final String LINKED_LIST = "linked_list";
    public static final String SORTING = "sorting";

    private TutorKnowledgeNormalizer() {
    }

    /**
     * @return canonical key，如 binary_tree；无法识别时返回小写化后的安全片段或 unknown
     */
    public static String normalize(String knowledgePoint) {
        if (knowledgePoint == null || knowledgePoint.isBlank()) {
            return UNKNOWN;
        }
        String t = knowledgePoint.trim();
        String lower = t.toLowerCase();
        if (lower.equals(BINARY_TREE) || t.contains("二叉树")) {
            return BINARY_TREE;
        }
        if (lower.equals(LINKED_LIST) || t.contains("链表")) {
            return LINKED_LIST;
        }
        if (lower.equals(SORTING) || t.contains("排序")) {
            return SORTING;
        }
        if (lower.contains("binary") && lower.contains("tree")) {
            return BINARY_TREE;
        }
        if (lower.contains("linked") && lower.contains("list")) {
            return LINKED_LIST;
        }
        if (lower.contains("sort")) {
            return SORTING;
        }
        return lower.replaceAll("\\s+", "_").replaceAll("[^a-z0-9_]", "");
    }
}
