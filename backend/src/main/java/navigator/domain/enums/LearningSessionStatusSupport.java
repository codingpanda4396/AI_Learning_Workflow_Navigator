package navigator.domain.enums;

/**
 * 学习会话状态字符串与 {@link LearningSessionStatus} 对齐的解析与判断。
 * 持久化与 API 仍使用枚举 name() 作为存取值。
 */
public final class LearningSessionStatusSupport {

    private LearningSessionStatusSupport() {
    }

    public static boolean isCompletedOrReportReady(String raw) {
        if (raw == null) {
            return false;
        }
        return LearningSessionStatus.COMPLETED.name().equals(raw)
                || LearningSessionStatus.REPORT_READY.name().equals(raw);
    }

    /**
     * 与 SessionStateGuard / Execution 流状态一致：可视为报告阶段或进度已跑满。
     */
    public static boolean isReportReady(String rawStatus, int completedTaskCount, int totalTaskCount) {
        return isCompletedOrReportReady(rawStatus)
                || (totalTaskCount > 0 && completedTaskCount >= totalTaskCount);
    }

    public static boolean isDiagnosisCompleted(String raw) {
        return raw != null && LearningSessionStatus.DIAGNOSIS_COMPLETED.name().equals(raw);
    }
}
