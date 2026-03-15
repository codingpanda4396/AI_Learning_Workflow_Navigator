package com.pandanav.learning.application.service;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Resolves topic-specific sub-focus labels for q_topic_focus (knowledge-point-level probe).
 * Rule-based: match topic/chapter/goal keywords to predefined sub-focus lists.
 */
@Component
public class TopicSubFocusResolver {

    /** Topic keyword -> list of sub-focus labels (e.g. "图的表示与遍历", "最短路径算法流程"). */
    private static final Map<String, List<String>> TOPIC_SUB_FOCUS = Map.ofEntries(
        Map.entry("图", List.of("图的表示与存储", "图的遍历", "最短路径算法流程", "应用场景与适用条件", "其他")),
        Map.entry("最短路径", List.of("图的表示与遍历", "最短路径算法思想", "算法流程与实现", "应用场景与适用条件", "其他")),
        Map.entry("树", List.of("树的表示与遍历", "二叉树与性质", "常见算法与应用", "其他")),
        Map.entry("链表", List.of("链表基本操作", "指针与边界", "常见题型", "其他")),
        Map.entry("排序", List.of("排序思想与稳定性", "时间与空间复杂度", "应用场景", "其他")),
        Map.entry("动态规划", List.of("状态定义与转移", "边界与优化", "经典模型", "其他"))
    );

    private static final List<String> DEFAULT_SUB_FOCUS = List.of(
        "概念与原理", "流程与实现", "应用与边界", "其他"
    );

    /**
     * Returns 3～5 sub-focus labels for the given topic and optional goal.
     */
    public List<String> resolve(String topic, String goal) {
        String combined = (topic != null ? topic : "") + " " + (goal != null ? goal : "");
        String lower = combined.toLowerCase(Locale.ROOT).trim();
        if (lower.isEmpty()) {
            return new ArrayList<>(DEFAULT_SUB_FOCUS);
        }
        for (Map.Entry<String, List<String>> e : TOPIC_SUB_FOCUS.entrySet()) {
            if (lower.contains(e.getKey())) {
                return new ArrayList<>(e.getValue());
            }
        }
        return new ArrayList<>(DEFAULT_SUB_FOCUS);
    }
}
