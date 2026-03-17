package navigator.domain.enums;

/**
 * 学习偏好，从 goal.preferenceTags 推导。
 * 供 PlanTemplateFactory 编排顺序消费。
 */
public enum LearningPreference {
    CONCEPT_FIRST,
    EXAMPLE_FIRST,
    PRACTICE_FIRST,
    STEP_BY_STEP,
    FRAMEWORK_FIRST,
    BALANCED
}
