package com.pandanav.learning.domain.enums;

public enum DiagnosisDimension {
    FOUNDATION,
    EXPERIENCE,
    GOAL_STYLE,
    TIME_BUDGET,
    LEARNING_PREFERENCE,
    DIFFICULTY_PAIN_POINT,
    /** 通用题：当前最大卡点 */
    BLOCKER,
    /** 通用题：练习量 */
    PRACTICE,
    /** 通用题：学习方式偏好 */
    PREFERENCE,
    /** 通用题：学习目标 */
    GOAL,
    /** 主题题：核心概念把握 */
    TOPIC_CORE,
    /** 主题题：实际操作卡点 */
    TOPIC_OPERATION,
    /** 主题题：学习顺序偏好（先原理还是先代码） */
    TOPIC_FOCUS
}
