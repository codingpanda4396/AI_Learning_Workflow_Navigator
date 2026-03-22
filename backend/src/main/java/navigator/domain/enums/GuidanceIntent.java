package navigator.domain.enums;

/**
 * 规则选定的导师意图；LLM 仅负责自然语言化。
 */
public enum GuidanceIntent {
    ASK_CLARIFYING_QUESTION,
    REQUEST_SELF_ARTICULATION,
    GIVE_SCAFFOLD_HINT,
    REDIRECT_OFF_TASK,
    NUDGE_DECOMPOSE,
    SUMMARY_PREP,
    CORRECT_MISCONCEPTION_LIGHT
}
