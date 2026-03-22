package navigator.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import navigator.domain.enums.GuidanceIntent;
import navigator.domain.enums.LearningActionType;
import navigator.domain.enums.LearningGuidancePhase;

/**
 * 持久化至 task_message.metadata_json。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskMessageMetadata {
    private String messageType;
    private LearningGuidancePhase guidancePhase;
    private GuidanceIntent guidanceIntent;
    private LearningActionType userActionType;
    private Boolean userAskedActively;
    private Boolean vagueAnswer;
    private Boolean summaryProduced;
    private Boolean hintGiven;
    private Boolean directAnswerRisk;
    private String taskExecutionStateBefore;
    private String taskExecutionStateAfter;
}
