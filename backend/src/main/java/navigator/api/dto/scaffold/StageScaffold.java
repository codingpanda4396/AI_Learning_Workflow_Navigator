package navigator.api.dto.scaffold;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StageScaffold {
    private String stageKey;
    private String stageTitle;
    private String stageGoal;
    private String stageDescription;
    private List<LearningActionCard> actionCards;
    private String validatorType;
    private String tutorMode;
    /** 当前应完成的动作卡（服务端进度） */
    private String currentActionId;
    /** STRUCTURE 是否已全部通过 */
    private Boolean structureStageComplete;
    /** UNDERSTANDING 是否已全部通过 */
    private Boolean understandingStageComplete;
    /** TRAINING 是否已全部通过 */
    private Boolean trainingStageComplete;
    /** REFLECTION 是否已全部通过 */
    private Boolean reflectionStageComplete;
    @Builder.Default
    private List<String> completedStageKeys = new ArrayList<>();
    private ReflectionRecord reflectionRecord;
    private ReflectionInsight reflectionInsight;

    /** STRUCTURE 脚手架：已探索的 promptKey */
    @Builder.Default
    private List<String> structureExploredPromptKeys = new ArrayList<>();
    private Integer structureGenerationCount;
    private Integer structureLightInteractionCount;
    private Boolean structureCanComplete;
    private String structureLastPromptKey;
}
