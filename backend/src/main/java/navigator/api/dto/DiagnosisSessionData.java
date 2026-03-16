package navigator.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import navigator.domain.model.DiagnosisQuestion;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiagnosisSessionData {
    private String diagnosisId;
    private String sessionId;
    private String status;
    private String generationMode;
    private List<DiagnosisQuestion> questions;
}
