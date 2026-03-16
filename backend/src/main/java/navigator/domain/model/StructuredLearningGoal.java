package navigator.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import navigator.domain.enums.GoalType;
import navigator.domain.enums.PreferenceTag;
import navigator.domain.enums.SelfReportedLevel;
import navigator.domain.enums.TimeBudget;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StructuredLearningGoal {
    private String rawGoalText;
    private String normalizedGoalText;
    private GoalType goalType;
    private String subject;
    private String topicScopeType;
    private List<String> topics;
    private String intentDescription;
    private TimeBudget timeBudget;
    private String urgencyLevel;
    private String expectedDepth;
    private SelfReportedLevel selfReportedLevel;
    private List<PreferenceTag> preferenceTags;
    private List<String> constraints;
    private String sourceContext;
}
