package navigator.domain.model;

/**
 * 脚手架动作运行时状态（字符串与前端/持久化对齐）。
 */
public final class ScaffoldRuntimeStatus {
    public static final String NOT_STARTED = "NOT_STARTED";
    public static final String IN_PROGRESS = "IN_PROGRESS";
    public static final String REVISION_REQUIRED = "REVISION_REQUIRED";
    public static final String PASSED = "PASSED";
    public static final String COMPLETED = "COMPLETED";

    private ScaffoldRuntimeStatus() {
    }
}
