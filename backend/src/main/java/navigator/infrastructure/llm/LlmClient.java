package navigator.infrastructure.llm;

import reactor.core.publisher.Flux;

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
    Flux<String> chatStream(String systemPrompt, String userPrompt);

    /**
     * 反馈同步路径：固定短读超时，避免用户等待抖动。
     */
    String chatForFeedback(String systemPrompt, String userPrompt);

    /**
     * 脚手架 JSON 生成路径：使用更高的 max_tokens 以避免 JSON 截断。
     */
    String chatForScaffold(String systemPrompt, String userPrompt);

    /** 是否应尝试调用真实供应商（启用且 apiKey 非空）。 */
    boolean isLiveProviderReady();
}
