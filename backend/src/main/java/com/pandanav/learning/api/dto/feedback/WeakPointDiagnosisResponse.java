package com.pandanav.learning.api.dto.feedback;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record WeakPointDiagnosisResponse(
    @JsonProperty("session_id")
    Long sessionId,
    @JsonProperty("diagnosis_summary")
    String diagnosisSummary,
    @JsonProperty("weak_nodes")
    List<WeakPointNodeResponse> weakNodes
) {
}
