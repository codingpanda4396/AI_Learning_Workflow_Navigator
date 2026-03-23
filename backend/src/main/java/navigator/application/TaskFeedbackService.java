package navigator.application;

import navigator.api.dto.TaskFeedbackResponse;
import org.springframework.stereotype.Service;

/**
 * 任务作答反馈（R0002 mock）：多信号规则判定，不调用 LLM。
 */
@Service
public class TaskFeedbackService {

    private static final String KEYWORD_TREE = "二叉树";

    public TaskFeedbackResponse evaluate(String answer) {
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
                    null);
        }

        if (mentionsTwoChildren) {
            return new TaskFeedbackResponse(
                    true,
                    "你已经同时提到「二叉树」和「两个子节点」的限制，结构把握很完整。",
                    "可以试着用自己的话点一下：叶子节点在你心里的画面是什么？",
                    "你已经抓住了树形结构和「最多两个子节点」两个要点。",
                    null,
                    "若愿意写得更具体，可以补一句生活或图形类比，帮助以后回忆。");
        }

        return new TaskFeedbackResponse(
                true,
                "你已经抓住「二叉树」这一核心概念。",
                "建议再补一句：每个节点最多有两个子节点，这是和二叉树强相关的定义点。",
                "你已经抓住了「树结构」这一核心。",
                "还可以更清晰：你还没有明确提到「每个节点最多有两个子节点」。",
                "建议在答案里补充：「每个节点最多有两个子节点」。");
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
        // 宽松：同时出现「两个」与「子节点」
        return text.contains("两个") && text.contains("子节点");
    }
}
