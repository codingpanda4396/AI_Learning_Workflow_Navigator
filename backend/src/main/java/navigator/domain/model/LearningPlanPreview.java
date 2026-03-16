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
public class LearningPlanPreview {
    private String planId;
    private String goalId;
    private RecommendedEntry recommendedEntry;
    private RecommendedStrategy recommendedStrategy;
    private List<PlanStage> stages;
    private List<TaskBlueprint> tasks;
    private List<String> successCriteria;
    private List<String> keyEvidence;
    private List<String> risks;
    private boolean previewOnly;
}
