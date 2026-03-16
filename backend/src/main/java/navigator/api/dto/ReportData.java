package navigator.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import navigator.domain.model.LearningReport;
import navigator.domain.model.NextActionDecision;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportData {
    private LearningReport learningReport;
    private NextActionDecision nextActionDecision;
}
