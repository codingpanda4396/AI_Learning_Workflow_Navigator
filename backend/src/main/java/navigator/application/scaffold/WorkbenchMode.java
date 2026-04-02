package navigator.application.scaffold;

/**
 * GET /scaffold 工作台组装模式：fast 不阻塞 LLM；full 含软文案（缓存命中则快）。
 */
public enum WorkbenchMode {
    FAST,
    FULL;

    public static WorkbenchMode fromQueryParam(String raw) {
        if (raw == null || raw.isBlank()) {
            return FULL;
        }
        return "fast".equalsIgnoreCase(raw.trim()) ? FAST : FULL;
    }
}
