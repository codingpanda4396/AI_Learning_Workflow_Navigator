package navigator.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import navigator.domain.enums.GuidanceIntent;
import navigator.domain.enums.LearningGuidancePhase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GuidanceDecision {
    private LearningGuidancePhase phase;
    private GuidanceIntent intent;
    @Builder.Default
    private boolean allowSubstantiveAnswer = false;
    @Builder.Default
    private List<String> mandatoryBehaviors = new ArrayList<>();
    private String policyRuleId;
    @Builder.Default
    private Map<String, String> promptSlots = new HashMap<>();
}
