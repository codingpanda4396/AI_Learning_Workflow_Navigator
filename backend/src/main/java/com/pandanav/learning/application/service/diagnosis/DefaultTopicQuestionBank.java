package com.pandanav.learning.application.service.diagnosis;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 数据结构课程常见主题题池，首期覆盖：数组、链表、栈、队列、二叉树、图、哈希表、排序。
 * 无匹配时使用通用模板。主题题文案做自然语言改写，避免生硬拼接「目标 + 主题」。
 */
@Component
public class DefaultTopicQuestionBank implements TopicQuestionBank {

    private static final String TOPIC_CORE_TITLE_TEMPLATE = "你对「{topic}」的核心概念把握更接近哪种状态？";
    private static final String TOPIC_OPERATION_TITLE_TEMPLATE = "一到「{topic}」的做题或实现时，你更容易卡在哪儿？";
    private static final String TOPIC_FOCUS_TITLE_TEMPLATE = "学「{topic}」时你更想先搞清原理还是先会写代码？";

    private static final Map<String, String> TOPIC_CORE_TITLES = new LinkedHashMap<>();
    private static final Map<String, String> TOPIC_OPERATION_TITLES = new LinkedHashMap<>();

    static {
        String[] topics = { "数组", "链表", "栈", "队列", "二叉树", "图", "哈希表", "排序" };
        for (String t : topics) {
            TOPIC_CORE_TITLES.put(t, "你对「" + t + "」的核心概念把握更接近哪种状态？");
            TOPIC_OPERATION_TITLES.put(t, "一到「" + t + "」的做题或实现时，你更容易卡在哪儿？");
        }
    }

    private static String fillTemplate(String template, String topic) {
        return template.replace("{topic}", topic == null || topic.isBlank() ? "当前主题" : topic);
    }

    @Override
    public String topicCoreTitle(String topicTitle) {
        if (topicTitle == null || topicTitle.isBlank()) {
            return fillTemplate(TOPIC_CORE_TITLE_TEMPLATE, "当前主题");
        }
        String normalized = topicTitle.trim();
        return TOPIC_CORE_TITLES.getOrDefault(normalized, fillTemplate(TOPIC_CORE_TITLE_TEMPLATE, normalized));
    }

    @Override
    public String topicOperationTitle(String topicTitle) {
        if (topicTitle == null || topicTitle.isBlank()) {
            return fillTemplate(TOPIC_OPERATION_TITLE_TEMPLATE, "当前主题");
        }
        String normalized = topicTitle.trim();
        return TOPIC_OPERATION_TITLES.getOrDefault(normalized, fillTemplate(TOPIC_OPERATION_TITLE_TEMPLATE, normalized));
    }

    @Override
    public boolean supportsTopic(String topicTitle) {
        return topicTitle != null && TOPIC_CORE_TITLES.containsKey(topicTitle.trim());
    }

    @Override
    public String topicCoreTitleWithGoal(String goalText, String topicTitle) {
        String topic = (topicTitle == null || topicTitle.isBlank()) ? "当前主题" : topicTitle.trim();
        if (goalText == null || goalText.isBlank()) {
            return topicCoreTitle(topicTitle);
        }
        if (isGraphOrShortestPath(goalText, topic)) {
            return "你对图、路径、权重这些概念熟吗？";
        }
        if (isTreeRelated(goalText, topic)) {
            return "你对节点、遍历、递归这些概念熟吗？";
        }
        if (isSortOrSearch(goalText, topic)) {
            return "你对比较、顺序、边界这些概念熟吗？";
        }
        return "你对「" + topic + "」的核心概念把握更接近哪种状态？";
    }

    @Override
    public String topicOperationTitleWithGoal(String goalText, String topicTitle) {
        String topic = (topicTitle == null || topicTitle.isBlank()) ? "当前主题" : topicTitle.trim();
        if (goalText == null || goalText.isBlank()) {
            return topicOperationTitle(topicTitle);
        }
        if (isGraphOrShortestPath(goalText, topic)) {
            return "在写最短路径或类似题时，你更容易卡在哪儿？";
        }
        if (isTreeRelated(goalText, topic)) {
            return "在做树/图上的遍历或递归时，你更容易卡在哪儿？";
        }
        return "一到「" + topic + "」的做题或实现时，你更容易卡在哪儿？";
    }

    @Override
    public String topicFocusTitleWithGoal(String goalText, String topicTitle) {
        if (goalText != null && !goalText.isBlank()) {
            return "学这个时你更想先搞清原理还是先会写代码？";
        }
        String topic = (topicTitle == null || topicTitle.isBlank()) ? "当前主题" : topicTitle.trim();
        return fillTemplate(TOPIC_FOCUS_TITLE_TEMPLATE, topic);
    }

    private static boolean isGraphOrShortestPath(String goalText, String topic) {
        String g = goalText.toLowerCase();
        return g.contains("最短路径") || g.contains("图的") || g.contains("图论")
            || g.contains("dijkstra") || "图".equals(topic);
    }

    private static boolean isTreeRelated(String goalText, String topic) {
        String g = goalText.toLowerCase();
        return g.contains("二叉树") || g.contains("树") || g.contains("遍历")
            || "二叉树".equals(topic) || "树".equals(topic);
    }

    private static boolean isSortOrSearch(String goalText, String topic) {
        String g = goalText.toLowerCase();
        return g.contains("排序") || g.contains("查找") || g.contains("搜索")
            || "排序".equals(topic) || "哈希表".equals(topic);
    }
