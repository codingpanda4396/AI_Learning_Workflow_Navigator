package com.pandanav.learning.domain.llm.model;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record PersonalizedPathContext(
    String goalText,
    GoalDiagnosisSnapshot goalDiagnosis,
    Map<Long, BigDecimal> masteryByNode,
    List<String> recentErrorTags,
    List<Integer> recentScores,
    String weakPointsSummary,
    List<ChapterNodeSnapshot> chapterNodes
) {
    public record GoalDiagnosisSnapshot(
        int specificScore,
        int measurableScore,
        int achievableScore,
        int relevantScore,
        int timeBoundScore
    ) {
    }

    public record ChapterNodeSnapshot(
        Long nodeId,
        String nodeName,
        Integer orderNo
    ) {
    }
}
