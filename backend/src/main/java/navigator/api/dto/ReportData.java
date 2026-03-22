package navigator.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import navigator.domain.model.LearningReport;
import navigator.domain.model.NextActionDecision;

/**
 * GET /api/sessions/{sessionId}/report 响应。
 * 合同冻结：learningReport（含 resultStatus, completedProgress, unresolvedIssues, evidenceSummary）,
 * nextActionDecision（含 actionType, reason, requiresReplan）为稳定字段。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportData {
    private LearningReport learningReport;
    private NextActionDecision nextActionDecision;
}
