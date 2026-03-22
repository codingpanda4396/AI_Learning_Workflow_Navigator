package navigator.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import navigator.domain.enums.LearningGuidancePhase;

import java.util.LinkedHashSet;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskExecutionEvidenceSnapshot {
    @Builder.Default
    private int totalTurns = 0;
    @Builder.Default
    private int userInitiatedQuestionTurns = 0;
    @Builder.Default
    private int assistantHintTurns = 0;
    @Builder.Default
    private int assistantRedirectTurns = 0;
    @Builder.Default
    private int directAnswerSeekCount = 0;
    @Builder.Default
    private int vagueUserReplyCount = 0;
    @Builder.Default
    private int confusionSignalsCount = 0;

    @Builder.Default
    private Set<LearningGuidancePhase> completedGuidancePhases = new LinkedHashSet<>();
    @Builder.Default
    private boolean frameworkArticulated = false;
    @Builder.Default
    private boolean reflectionArticulated = false;
    @Builder.Default
    private boolean selfExplanationSubmitted = false;
    @Builder.Default
    private boolean checkpointPassed = false;

    /** 0–1 规则分，非 LLM */
    @Builder.Default
    private double directAnswerDependencyScore = 0.0;
}
