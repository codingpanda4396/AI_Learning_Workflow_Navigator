package navigator.application.scaffold;

/**
 * 反思校验机读码（供 {@link ReflectionTutorComposer} 映射短提示）。
 */
public final class ReflectionErrorTypes {
    public static final String TOO_SHORT = "REFLECTION_TOO_SHORT";
    public static final String GENERIC = "REFLECTION_GENERIC";
    public static final String DEFINITION_ONLY = "REFLECTION_DEFINITION_ONLY";
    public static final String NO_RULE_SHAPE = "REFLECTION_NO_RULE_SHAPE";
    public static final String CAPABILITY_VAGUE = "REFLECTION_CAPABILITY_VAGUE";
    public static final String INVALID_ACTION = "INVALID_ACTION";

    private ReflectionErrorTypes() {
    }
}
