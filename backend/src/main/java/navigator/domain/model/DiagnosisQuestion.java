package navigator.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiagnosisQuestion {
    private String questionId;
    private String dimension;
    private String type;
    private boolean required;
    private String title;
    private String description;
    private String whyAsking;
    private List<String> impactsPlanning;
    private List<DiagnosisOption> options;
}
