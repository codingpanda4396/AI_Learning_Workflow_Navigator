package navigator.application.support;

/**
 * 将 API 层字符串 id（如 learn_session_12、plan_3）解析为持久化数字主键。
 */
public final class ExternalIdSupport {

    private ExternalIdSupport() {
    }

    public static Long extractNumericId(String id) {
        if (id == null) {
            return null;
        }
        String digits = id.replaceAll("\\D+", "");
        if (digits.isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(digits);
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
