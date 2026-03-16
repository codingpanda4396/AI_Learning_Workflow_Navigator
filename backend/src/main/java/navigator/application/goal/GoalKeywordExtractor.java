package navigator.application.goal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Sprint 1: 从 rawGoalText 提取 topics，识别 CHAPTER/COURSE 等范围关键词。
 */
public final class GoalKeywordExtractor {

    private static final List<String> TOPIC_KEYWORDS = Arrays.asList(
            "链表", "栈", "队列", "树", "图", "排序", "查找", "数组", "哈希", "堆", "递归", "动态规划", "408", "数据结构"
    );

    private static final Pattern CHAPTER_PATTERN = Pattern.compile("这一章|章节|全章");
    private static final Pattern COURSE_PATTERN = Pattern.compile("408|整体|系统学习|一轮|全面|系统");

    private GoalKeywordExtractor() {
    }

    /**
     * 从文本中提取 1~3 个主题。若 topicHints 非空则直接返回（调用方传入）；否则按关键词表匹配。
     */
    public static List<String> extractTopics(String rawGoalText, List<String> topicHints) {
        if (rawGoalText == null) rawGoalText = "";
        if (topicHints != null && !topicHints.isEmpty()) {
            return new ArrayList<>(topicHints);
        }
        String text = rawGoalText.trim();
        List<String> found = new ArrayList<>();
        for (String kw : TOPIC_KEYWORDS) {
            if (text.contains(kw) && !found.contains(kw)) {
                found.add(kw);
                if (found.size() >= 3) break;
            }
        }
        if (found.isEmpty()) {
            return text.isEmpty() ? List.of("未指定主题") : List.of(text);
        }
        return found;
    }

    /**
     * 是否包含“章节/全章”类表述。
     */
    public static boolean isChapterScope(String rawGoalText) {
        return rawGoalText != null && CHAPTER_PATTERN.matcher(rawGoalText).find();
    }

    /**
     * 是否包含“408/系统学习/一轮”等课程级表述。
     */
    public static boolean isCourseScope(String rawGoalText) {
        return rawGoalText != null && COURSE_PATTERN.matcher(rawGoalText).find();
    }
}
