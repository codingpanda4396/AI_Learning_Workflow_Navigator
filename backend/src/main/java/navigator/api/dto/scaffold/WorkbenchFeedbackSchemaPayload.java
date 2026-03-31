package navigator.api.dto.scaffold;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 约束反馈区展示结构（软提示）；具体反馈内容见 {@link StructuredScaffoldFeedbackPayload}。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkbenchFeedbackSchemaPayload {
    private String completenessLabel;
    private String issuePointsLabel;
    private String minimalRevisionLabel;
    private String nextActionLabel;
    /** 最多展示几条 issue（默认 3） */
    private Integer maxIssuePoints;
}
