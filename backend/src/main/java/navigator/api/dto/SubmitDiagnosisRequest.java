package navigator.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import navigator.domain.model.DiagnosisAnswer;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubmitDiagnosisRequest {
    private String diagnosisId;
    private List<DiagnosisAnswer> answers;
}
