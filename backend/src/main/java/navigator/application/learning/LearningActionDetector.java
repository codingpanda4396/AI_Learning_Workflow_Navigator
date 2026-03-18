package navigator.application.learning;

import navigator.domain.enums.LearningActionType;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 规则优先的学习动作识别。
 */
@Component
public class LearningActionDetector {

    private static final Pattern SELF_EXPLAIN = Pattern.compile(
            "我这样理解|我的理解是|我认为|总结一下|换句话说|也就是说");
    private static final Pattern DIRECT_ANSWER = Pattern.compile(
            "直接给.*答案|告诉我答案|标准答案|完整代码|帮我写完");
    private static final Pattern OFF_TOPIC = Pattern.compile(
            "天气|吃饭|游戏|无聊|闲聊");

    public LearningActionType detect(String content) {
        if (content == null || content.isBlank()) {
            return LearningActionType.GENERIC;
        }
        String t = content.trim();
        String lower = t.toLowerCase(Locale.ROOT);
        if (OFF_TOPIC.matcher(t).find()) {
            return LearningActionType.OFF_TOPIC;
        }
        if (DIRECT_ANSWER.matcher(t).find()) {
            return LearningActionType.SEEK_DIRECT_ANSWER;
        }
        if (SELF_EXPLAIN.matcher(t).find()) {
            return LearningActionType.SELF_EXPLANATION;
        }
        if (t.contains("举例") || t.contains("例子") || lower.contains("example")) {
            return LearningActionType.ASK_FOR_EXAMPLE;
        }
        if (t.contains("对比") || t.contains("比较") || t.contains("差异") || t.contains("区别")) {
            return LearningActionType.ASK_FOR_COMPARISON;
        }
        if (t.contains("更简单") || t.contains("通俗") || t.contains("听不懂") || t.contains("太抽象")) {
            return LearningActionType.ASK_FOR_SIMPLIFICATION;
        }
        if (t.contains("不懂") || t.contains("不明白") || t.contains("还是不懂") || t.contains("看不懂")) {
            return LearningActionType.CONFUSION_SIGNAL;
        }
        if (t.contains("为什么") || t.contains("是什么") || t.contains("怎么理解") || t.contains("解释")) {
            return LearningActionType.ASK_FOR_EXPLANATION;
        }
        return LearningActionType.GENERIC;
    }
}
