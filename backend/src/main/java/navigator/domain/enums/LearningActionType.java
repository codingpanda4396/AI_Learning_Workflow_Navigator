package navigator.domain.enums;

/**
 * 学习动作识别枚举（规则优先）。
 */
public enum LearningActionType {
    ASK_FOR_EXPLANATION,
    ASK_FOR_EXAMPLE,
    ASK_FOR_COMPARISON,
    ASK_FOR_SIMPLIFICATION,
    SELF_EXPLANATION,
    ANSWER_CHECK,
    SEEK_DIRECT_ANSWER,
    OFF_TOPIC,
    CONFUSION_SIGNAL,
    /** 无法归类 */
    GENERIC
}
