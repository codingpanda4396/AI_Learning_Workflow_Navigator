package navigator.api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import navigator.domain.model.GoalContextSnapshot;
import navigator.domain.model.StructuredLearningGoal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateGoalData {
    private String goalId;
    private StructuredLearningGoal structuredGoal;
    private GoalContextSnapshot goalContextSnapshot;
}
