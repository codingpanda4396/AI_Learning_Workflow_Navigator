package navigator.application.task.guidance;

import navigator.domain.enums.LearningGuidancePhase;
import org.springframework.stereotype.Component;

/**
 * 探索轮次驱动的引导阶段（规则，非 LLM）。在 {@code exploreTurnCount} 递增后调用。
 */
@Component
public class GuidancePhaseTransitionEvaluator {

    public LearningGuidancePhase phaseForExploreCount(int exploreTurns) {
        if (exploreTurns <= 1) {
            return LearningGuidancePhase.CLARIFY_GOAL;
        }
        if (exploreTurns == 2) {
            return LearningGuidancePhase.BUILD_FRAME;
        }
        if (exploreTurns == 3) {
            return LearningGuidancePhase.TRY_EXPRESS;
        }
        if (exploreTurns == 4) {
            return LearningGuidancePhase.PROBE_GAPS;
        }
        if (exploreTurns == 5) {
            return LearningGuidancePhase.META_REFLECT;
        }
        return LearningGuidancePhase.TRANSITION_HINT;
    }
}
