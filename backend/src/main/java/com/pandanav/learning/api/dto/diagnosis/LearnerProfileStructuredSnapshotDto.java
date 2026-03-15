package com.pandanav.learning.api.dto.diagnosis;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * 诊断提交后产出的结构化画像快照，落库并供规划页消费。
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LearnerProfileStructuredSnapshotDto(
    @JsonProperty("profileVersion") String profileVersion,
    @JsonProperty("foundationLevel") String foundationLevel,
    @JsonProperty("primaryBlocker") String primaryBlocker,
    @JsonProperty("practiceLevel") String practiceLevel,
    @JsonProperty("learningPreference") String learningPreference,
    @JsonProperty("goalType") String goalType,
    @JsonProperty("timeBudget") String timeBudget,
    @JsonProperty("topicConceptClarity") String topicConceptClarity,
    @JsonProperty("topicOperationRisk") String topicOperationRisk,
    @JsonProperty("riskTags") List<String> riskTags,
    @JsonProperty("planHints") PlanHintsDto planHints,
    @JsonProperty("summary") SummaryDto summary
) {
    public record PlanHintsDto(
        @JsonProperty("entryMode") String entryMode,
        @JsonProperty("explanationStyle") String explanationStyle,
        @JsonProperty("pace") String pace,
        @JsonProperty("taskGranularity") String taskGranularity,
        @JsonProperty("focusMode") String focusMode
    ) {}

    public record SummaryDto(
        @JsonProperty("currentState") String currentState,
        @JsonProperty("evidence") List<String> evidence
    ) {}
}
