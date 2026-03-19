package navigator.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import navigator.domain.model.DiagnosisEvidenceSummary;
import navigator.domain.model.LearnerProfileSnapshot;
import navigator.domain.model.LearnerStrategyProfile;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmitDiagnosisData {
    private String diagnosisId;
    private LearnerProfileSnapshot learnerProfileSnapshot;
    private LearnerStrategyProfile learnerStrategyProfile;
    private DiagnosisEvidenceSummary diagnosisEvidenceSummary;
}
