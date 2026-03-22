package navigator.domain.enums;

/**
 * 引导子阶段：依附 EXPLORE/REMEDIAL，与 {@link TaskExecutionState} 正交。
 */
public enum LearningGuidancePhase {
    CLARIFY_GOAL,
    BUILD_FRAME,
    TRY_EXPRESS,
    PROBE_GAPS,
    META_REFLECT,
    TRANSITION_HINT
}
