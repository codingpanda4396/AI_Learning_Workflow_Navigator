package navigator.infrastructure.llm;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * R0003：OpenAI 兼容 Chat Completions 抽象；与业务阶段无关的纯技术出口。
 */
public interface LlmClient {

    /**
     * 较长读超时（由配置 navigator.llm.timeout-ms 决定），供异步 explain 等使用。
     */
    String chat(String systemPrompt, String userPrompt);

    /**
     * 流式输出：按供应商返回的增量调用 {@code onDelta}（可能含多段空字符串，由实现过滤）。
     * 仅在 {@link #isLiveProviderReady()} 为 true 时调用；否则由业务层走兜底，不调用本方法。
     */
    void chatStream(String systemPrompt, String userPrompt, Consumer<String> onDelta)
            throws IOException, InterruptedException;

    /**
     * 反馈同步路径：固定短读超时（2s），避免用户等待抖动。
     */
    String chatForFeedback(String systemPrompt, String userPrompt);

    /** 是否应尝试调用真实供应商（启用且 apiKey 非空）。 */
    boolean isLiveProviderReady();
}
