package navigator.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import navigator.domain.model.PlanStage;
import navigator.domain.model.RecommendedEntry;
import navigator.domain.model.RecommendedStrategy;
import navigator.domain.model.TaskBlueprint;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlanPreviewData {
    private String planId;
    private String status;
    private boolean previewOnly;
    private boolean committed;
    private String knowledgeKey;
    private String packId;
    private String knowledgeType;
    private String displayMode;
    private List<String> phaseHighlights;
    private List<String> commonMisconceptions;
    private String goal;
    private RecommendedEntry recommendedEntry;
    private RecommendedStrategy recommendedStrategy;
    private List<PlanStage> stages;
    private List<TaskBlueprint> tasks;
    private List<String> successCriteria;
    private List<String> keyEvidence;
    private List<String> risks;
}
