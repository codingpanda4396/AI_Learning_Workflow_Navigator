package com.pandanav.learning.api.dto.session;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record GoalDiagnosisResponse(
    @JsonProperty("goal_score")
    Integer goalScore,
    @JsonProperty("smart_breakdown")
    SmartBreakdown smartBreakdown,
    SummaryResponse feedback
) {

    public record SmartBreakdown(
        @JsonProperty("specific_score")
        Integer specificScore,
        @JsonProperty("measurable_score")
        Integer measurableScore,
        @JsonProperty("achievable_score")
        Integer achievableScore,
        @JsonProperty("relevant_score")
        Integer relevantScore,
        @JsonProperty("time_bound_score")
        Integer timeBoundScore
    ) {
    }

    public record SummaryResponse(
        String summary,
        List<String> strengths,
        List<String> risks,
        @JsonProperty("rewritten_goal")
        String rewrittenGoal
    ) {
    }
}
