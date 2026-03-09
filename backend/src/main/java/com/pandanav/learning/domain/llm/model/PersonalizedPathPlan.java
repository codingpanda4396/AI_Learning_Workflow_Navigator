package com.pandanav.learning.domain.llm.model;

import java.util.List;

public record PersonalizedPathPlan(
    List<OrderedNode> orderedNodes,
    List<InsertedTask> insertedTasks,
    String planReasoningSummary,
    List<String> riskFlags
) {
    public record OrderedNode(
        Long nodeId,
        Integer priority,
        String reason
    ) {
    }

    public record InsertedTask(
        Long nodeId,
        String stage,
        String objective,
        String trigger
    ) {
    }
}
