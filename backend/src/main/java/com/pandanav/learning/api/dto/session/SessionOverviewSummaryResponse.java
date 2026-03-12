package com.pandanav.learning.api.dto.session;

import com.fasterxml.jackson.annotation.JsonProperty;

public record SessionOverviewSummaryResponse(
    @JsonProperty("current_task_title")
    String currentTaskTitle,
    @JsonProperty("current_task_description")
    String currentTaskDescription,
    @JsonProperty("next_step_hint")
    String nextStepHint,
    @JsonProperty("primary_action_label")
    String primaryActionLabel,
    @JsonProperty("primary_action_path")
    String primaryActionPath,
    @JsonProperty("recent_report_summary")
    String recentReportSummary
) {
}
