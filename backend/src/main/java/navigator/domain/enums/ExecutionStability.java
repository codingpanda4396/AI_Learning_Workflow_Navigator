package navigator.domain.enums;

/**
 * 执行稳定性，从 gap + confidence 推导。
 * 供 PlanStrategySelector、任务粒度决策消费。
 */
public enum ExecutionStability {
    STABLE,
    MODERATE,
    UNSTABLE
}
