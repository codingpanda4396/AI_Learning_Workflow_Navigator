package navigator.application.tutor;

/**
 * Prompt / Explain 同步结果（source 供 API：CACHE、FALLBACK；Prompt 模板首次写入缓存仍报 FALLBACK）。
 */
public record AiTutorTextResult(String source, String content) {
}
