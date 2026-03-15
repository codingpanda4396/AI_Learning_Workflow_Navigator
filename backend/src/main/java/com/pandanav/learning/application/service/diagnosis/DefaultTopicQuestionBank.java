package com.pandanav.learning.application.service.diagnosis;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 数据结构课程常见主题题池，首期覆盖：数组、链表、栈、队列、二叉树、图、哈希表、排序。
 * 无匹配时使用通用模板。
 */
@Component
public class DefaultTopicQuestionBank implements TopicQuestionBank {

    private static final String TOPIC_CORE_TITLE_TEMPLATE = "关于「{topic}」，你对核心组成或基本概念的把握更接近哪种状态？";
    private static final String TOPIC_OPERATION_TITLE_TEMPLATE = "一到「{topic}」的实际操作或题目应用时，你更容易卡在哪？";

    private static final Map<String, String> TOPIC_CORE_TITLES = new LinkedHashMap<>();
    private static final Map<String, String> TOPIC_OPERATION_TITLES = new LinkedHashMap<>();

    static {
        String[] topics = { "数组", "链表", "栈", "队列", "二叉树", "图", "哈希表", "排序" };
        for (String t : topics) {
            TOPIC_CORE_TITLES.put(t, "关于「" + t + "」，你对核心组成或基本概念的把握更接近哪种状态？");
            TOPIC_OPERATION_TITLES.put(t, "一到「" + t + "」的实际操作或题目应用时，你更容易卡在哪？");
        }
    }

    private static String fillTemplate(String template, String topic) {
        return template.replace("{topic}", topic);
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
        String topic = (topicTitle == null || topicTitle.isBlank()) ? "主题" : topicTitle;
        return "关于「" + goalText + "」相关的" + topic + "基础，你对核心概念的把握更接近哪种状态？";
    }

    @Override
    public String topicOperationTitleWithGoal(String goalText, String topicTitle) {
        String topic = (topicTitle == null || topicTitle.isBlank()) ? "主题" : topicTitle;
        return "一到「" + goalText + "」与「" + topic + "」的实际应用时，你更容易卡在哪？";
    }
}
