package navigator.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import navigator.domain.enums.ResultStatus;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningReport {
    private String sessionId;
    private ResultStatus resultStatus;
    private String goalReview;
    private List<String> completedProgress;
    private List<String> unresolvedIssues;
    private List<String> evidenceSummary;
    private String summaryText;
    private NextActionDecision nextAction;
    /** 会话级学习方法汇总（Sprint 3） */
    private LearningMethodProfile learningMethodProfile;
}
