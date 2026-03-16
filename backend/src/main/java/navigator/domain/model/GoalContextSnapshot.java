package navigator.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import navigator.domain.enums.EntryGranularity;
import navigator.domain.enums.PlanningMode;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoalContextSnapshot {
    private StructuredLearningGoal structuredGoal;
    private boolean requiresDiagnosis;
    private PlanningMode planningMode;
    private EntryGranularity entryGranularity;
    private List<String> strategyHints;
    private List<String> riskTags;
    private List<String> explanationFocus;
    private String createdFrom;
    private Integer version;
}
