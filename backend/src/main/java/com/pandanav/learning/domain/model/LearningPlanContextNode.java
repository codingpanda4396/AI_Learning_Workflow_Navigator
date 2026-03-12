package com.pandanav.learning.domain.model;

import java.util.List;

public record LearningPlanContextNode(
    String planNodeId,
    Long persistedNodeId,
    String nodeName,
    Integer orderNo,
    Integer difficulty,
    Integer mastery,
    Integer attemptCount,
    List<String> weakReasons,
    List<String> recentErrorTags,
    List<String> prerequisiteNodeIds
) {
}
