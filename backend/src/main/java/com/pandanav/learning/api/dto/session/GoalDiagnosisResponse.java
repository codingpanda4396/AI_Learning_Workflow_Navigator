package com.pandanav.learning.api.dto.session;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record GoalDiagnosisResponse(
    @JsonProperty("goal_score")
    Integer goalScore,
    SummaryResponse feedback
) {
    public record SummaryResponse(
        String summary,
        List<String> strengths,
        List<String> risks,
        @JsonProperty("rewritten_goal")
        String rewrittenGoal
    ) {
    }
}

