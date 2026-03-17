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
public class LearningGoalInput {
    private String rawGoalText;
    private TimeBudget timeBudget;
    private SelfReportedLevel selfReportedLevel;
    private List<PreferenceTag> preferenceTags;
    private GoalType goalTypeHint;
    private String subjectHint;
    private List<String> topicHints;
    private String sourceContext;
    /** 优先模块/主题，可选；未传时由 topics[0] 推导 */
    private String priorityModule;
}
