package navigator.application.tutor.fallback;

import navigator.api.dto.TaskFeedbackResponse;
import org.springframework.stereotype.Component;

import java.util.Locale;

/**
 * R0003：写死的 explain / feedback 兜底（不依赖 LLM）。
 */
@Component
public class TutorFallbackRegistry {

    private static final String KEYWORD_TREE = "二叉树";

    public String explainFallback(String canonicalKnowledgeKey, String step) {
        String k = canonicalKnowledgeKey != null ? canonicalKnowledgeKey : TutorKnowledgeNormalizer.UNKNOWN;
        String st = step != null ? step.trim().toLowerCase(Locale.ROOT) : "";
        return switch (k) {
            case TutorKnowledgeNormalizer.BINARY_TREE -> binaryTreeExplain(st);
            case TutorKnowledgeNormalizer.LINKED_LIST -> linkedListExplain(st);
            case TutorKnowledgeNormalizer.SORTING -> sortingExplain(st);
            default -> "先用一句话说说你对这个概念最直观的画面，再对照任务目标看还缺哪一块。";
        };
    }

    private static String binaryTreeExplain(String step) {
        if ("step1".equals(step) || step.isEmpty()) {
            return "可以把二叉树想象成家谱，每个人最多有两个孩子。";
        }
        return "二叉树像分叉路线：每个路口最多分出两条路，走到底就是叶子。";
    }

    private static String linkedListExplain(String step) {
        if ("step1".equals(step) || step.isEmpty()) {
            return "链表像火车：一节车厢记着下一节在哪，不必所有车厢连续排在一起。";
        }
        return "链表的核心是「当前节点知道下一个是谁」，插入删除往往比连续数组更灵活。";
    }

    private static String sortingExplain(String step) {
        if ("step1".equals(step) || step.isEmpty()) {
            return "排序就是把元素按规则排成一队：先想清楚「比大小」的依据，再看怎么少搬动。";
        }
        return "看排序算法时，抓住两件事：比较/移动的次数级，以及是否用到额外空间。";
    }

    /**
     * LLM 已返回但 JSON 不可信时的反馈：按知识点给「仍在教你」的文案，避免只剩一句「解析失败」。
     */
    public TaskFeedbackResponse feedbackWhenLlmJsonUnreliable(String canonicalKnowledgeKey,
                                                              String step,
                                                              String userAnswer) {
        String k = canonicalKnowledgeKey != null ? canonicalKnowledgeKey : TutorKnowledgeNormalizer.UNKNOWN;
        if (TutorKnowledgeNormalizer.BINARY_TREE.equals(k)) {
            return new TaskFeedbackResponse(
                    false,
                    "你已经有基本理解了，但结构还不够清晰。",
                    "可以再描述一下每个节点最多连几个孩子、叶子在你心里长什么样。",
                    null,
                    null,
                    "如果一个节点有3个子节点还算二叉树吗？",
                    null);
        }
        if (TutorKnowledgeNormalizer.LINKED_LIST.equals(k)) {
            return new TaskFeedbackResponse(
                    false,
                    "你对链表的感觉已经有了，再把「谁指向谁」说清楚会更稳。",
                    "试着用「A 的 next 是 B」这种方式描述一小段。",
                    null,
                    null,
                    "如果只是顺序访问下一个元素，数组和链表你更在意哪一个成本？",
                    null);
        }
        if (TutorKnowledgeNormalizer.SORTING.equals(k)) {
            return new TaskFeedbackResponse(
                    false,
                    "排序思路已经在冒头了，再把「比较规则」钉死就更清楚。",
                    "用两个具体数字，走一遍你心里的比较顺序。",
                    null,
                    null,
                    "如果输入已经几乎有序，哪种想法会特别省事？",
                    null);
        }
        return feedbackFallback(k, step != null ? step : "", userAnswer);
    }

    /**
     * 反馈兜底：binary_tree 保留原 R0002 关键词启发；其余给短鼓励文案。
     */
    public TaskFeedbackResponse feedbackFallback(String canonicalKnowledgeKey, String step, String userAnswer) {
        String k = canonicalKnowledgeKey != null ? canonicalKnowledgeKey : TutorKnowledgeNormalizer.UNKNOWN;
        if (TutorKnowledgeNormalizer.BINARY_TREE.equals(k)) {
            return feedbackBinaryTreeHeuristic(userAnswer);
        }
        if (TutorKnowledgeNormalizer.LINKED_LIST.equals(k)) {
            return new TaskFeedbackResponse(
                    false,
                    "先抓住链表「节点 + 指针到下一节点」的画面。",
                    "用一句话说说：插入和删除时，你打算动哪几条链接？",
                    null,
                    null,
                    "试着画三个框，用箭头标 next。",
                    null);
        }
        if (TutorKnowledgeNormalizer.SORTING.equals(k)) {
            return new TaskFeedbackResponse(
                    false,
                    "先说明你比较的规则：按什么关键字、升序还是降序。",
                    "用两个具体数字走一遍你心里的步骤。",
                    null,
                    null,
                    "想一下：有没有「已经有序」时特别省事的边界情况？",
                    null);
        }
        return genericFeedbackFallback();
    }

    private static TaskFeedbackResponse feedbackBinaryTreeHeuristic(String answer) {
        String text = answer == null ? "" : answer.trim();
        boolean hasTree = text.contains(KEYWORD_TREE);
        boolean mentionsTwoChildren = mentionsTwoChildrenConstraint(text);

        if (!hasTree) {
            return new TaskFeedbackResponse(
                    false,
                    "还可以再贴近「二叉树」这个概念一点",
                    "试着在答案里用一句话说明：二叉树是什么、和「两个子节点」有什么关系",
                    null,
                    null,
                    null,
                    null);
        }

        if (mentionsTwoChildren) {
            return new TaskFeedbackResponse(
                    true,
                    "你已经同时提到「二叉树」和「两个子节点」的限制，结构把握很完整。",
                    "可以试着用自己的话点一下：叶子节点在你心里的画面是什么？",
                    "你已经抓住了树形结构和「最多两个子节点」两个要点。",
                    null,
                    "若愿意写得更具体，可以补一句生活或图形类比，帮助以后回忆。",
                    null);
        }

        return new TaskFeedbackResponse(
                true,
                "你已经抓住「二叉树」这一核心概念。",
                "建议再补一句：每个节点最多有两个子节点，这是和二叉树强相关的定义点。",
                "你已经抓住了「树结构」这一核心。",
                "还可以更清晰：你还没有明确提到「每个节点最多有两个子节点」。",
                "建议在答案里补充：「每个节点最多有两个子节点」。",
                null);
    }

    private static boolean mentionsTwoChildrenConstraint(String text) {
        if (text.contains("两个子节点")) {
            return true;
        }
        if (text.contains("最多两个") || text.contains("至多两个")) {
            return true;
        }
        if (text.contains("两个子")) {
            return true;
        }
        if (text.contains("两子")) {
            return true;
        }
        return text.contains("两个") && text.contains("子节点");
    }

    private static TaskFeedbackResponse genericFeedbackFallback() {
        return new TaskFeedbackResponse(
                false,
                "暂时无法获取导师反馈，我们先用结构化方式看一眼你的回答。",
                "用一句话重述你的核心观点，并标出一个你最不确定的词。",
                null,
                null,
                "试着先说出：这个概念让你联想到什么具体画面？",
                null);
    }

    /**
     * R00035：内嵌导师对话在 LLM 不可用时的短回复（单轮、不记忆）。
     */
    public String embeddedChatFallback(String canonicalKnowledgeKey, String userMessage) {
        String k = canonicalKnowledgeKey != null ? canonicalKnowledgeKey : TutorKnowledgeNormalizer.UNKNOWN;
        String hint = (userMessage == null || userMessage.isBlank())
                ? "先说说你对这个概念最直观的画面。"
                : "我听到你在用「" + shorten(userMessage, 24) + "」这类想法碰它，这很好。";
        if (TutorKnowledgeNormalizer.BINARY_TREE.equals(k)) {
            return hint + " 试着回答：二叉树里，每个节点最多连出几个孩子？叶子在你心里长什么样？";
        }
        if (TutorKnowledgeNormalizer.LINKED_LIST.equals(k)) {
            return hint + " 用「一节车厢指着下一节」类比时，插入中间要改几个指针？";
        }
        if (TutorKnowledgeNormalizer.SORTING.equals(k)) {
            return hint + " 先钉死比较规则：按什么关键字、升序还是降序？再用两个数字走一小步。";
        }
        return hint + " 把它压成一两句自己的话，再标出一个你最不确定的词。";
    }

    private static String shorten(String s, int max) {
        String t = s.trim().replaceAll("\\s+", " ");
        if (t.length() <= max) {
            return t;
        }
        return t.substring(0, max) + "…";
    }
}
