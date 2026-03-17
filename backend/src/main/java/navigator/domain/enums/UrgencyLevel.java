package navigator.domain.enums;

/**
 * 目标紧迫程度（goal intensity）。
 * 由 GoalRuleEngine 从 timeBudget + rawGoalText 推导，供 PlanStrategySelector、LearnerProfileSnapshot 消费。
 */
public enum UrgencyLevel {
    HIGH,
    MEDIUM,
    LOW
}
