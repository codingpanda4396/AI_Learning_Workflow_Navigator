package navigator.application.tutor;

/**
 * 内嵌导师流式输出：先 {@link #onMeta(String)}（source 如 LLM / FALLBACK），再多次 {@link #onDelta(String)}。
 */
public interface AiTutorChatStreamHandler {

    void onMeta(String source);

    void onDelta(String text);
}
