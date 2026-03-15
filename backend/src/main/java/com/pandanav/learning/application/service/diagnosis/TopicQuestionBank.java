package com.pandanav.learning.application.service.diagnosis;

import java.util.Map;
import java.util.Optional;

/**
 * 主题题池：首期支持数据结构课程常见主题，无匹配时使用通用模板回退。
 */
public interface TopicQuestionBank {

    /**
     * 主题题 1 标题模板，占位符 {topicTitle}。
     */
    String TOPIC_CORE_TITLE_TEMPLATE = "关于「{topicTitle}」，你对核心组成或基本概念的把握更接近哪种状态？";

    /**
     * 主题题 2 标题模板。
     */
    String TOPIC_OPERATION_TITLE_TEMPLATE = "一到「{topicTitle}」的实际操作或题目应用时，你更容易卡在哪？";

    /**
     * 返回主题题 1 的标题；有专属题池时用题池，否则用 TOPIC_CORE_TITLE_TEMPLATE。
     */
    String topicCoreTitle(String topicTitle);

    /**
     * 返回主题题 2 的标题。
     */
    String topicOperationTitle(String topicTitle);

    /**
     * 是否支持该主题（仅影响标题是否用通用模板，选项始终用 ContractCatalog 的 TOPIC_CORE/TOPIC_OPERATION）。
     */
    boolean supportsTopic(String topicTitle);

    static String fillTemplate(String template, String topicTitle) {
        String t = topicTitle == null ? "当前主题" : topicTitle.trim();
        return template.replace("{topicTitle}", t.isEmpty() ? "当前主题" : t);
    }
}
