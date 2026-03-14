package com.pandanav.learning.api.dto.plan;

import java.util.List;

public record LearningPlanLearnerSnapshotResponse(
    String goal,
    List<String> currentWeaknesses,
    Integer masteryScore,
    String riskIfSkipped,
    String currentFocusLabel
) {
}
