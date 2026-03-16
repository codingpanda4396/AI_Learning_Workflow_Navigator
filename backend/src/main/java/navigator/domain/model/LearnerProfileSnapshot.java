package navigator.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import navigator.domain.enums.ConfidenceLevel;
import navigator.domain.enums.FoundationLevel;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearnerProfileSnapshot {
    private String diagnosisId;
    private FoundationLevel foundationLevel;
    private ConfidenceLevel confidenceLevel;
    private String comprehensionPattern;
    private String executionPattern;
    private List<String> blockerTags;
    private List<String> riskTags;
    private String suggestedEntryStrategy;
    private String suggestedGranularity;
    private String suggestedFeedbackFrequency;
    private List<String> planningHints;
}
