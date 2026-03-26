package navigator.api.dto.scaffold;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LearningScaffoldActionResult {
    private ActionRuntime actionRuntime;
    private ValidationResult validation;
    private TutorResponse tutor;
    /** 本阶段是否全部完成（与 stageCompleted 同义，保留兼容） */
    private boolean stageComplete;
    /** TRAINING 专用结构化反馈 */
    private TrainingFeedback trainingFeedback;
    /** 与 stageComplete 对齐的显式字段 */
    private Boolean stageCompleted;
    /** 本动作卡是否本轮通过 */
    private Boolean actionCompleted;
    private String stageKey;
    private String actionId;
    private Integer attemptNo;
    private String runtimeStatus;
    private Boolean canProceed;
    private ReflectionRecord reflectionRecord;
    private ReflectionInsight reflectionInsight;
}
